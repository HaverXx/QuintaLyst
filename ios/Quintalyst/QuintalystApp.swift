import SwiftUI
import UIKit
import AVFoundation
import MediaPlayer
import WebKit
import Security
import QuintalystShared

@main struct QuintalystApp: App {
    @StateObject var style = StyleStore()
    @StateObject var player = PlayerStore()
    @StateObject var catalog = CatalogStore()
    var body: some Scene { WindowGroup { MainView().environmentObject(style).environmentObject(player).environmentObject(catalog).preferredColorScheme(style.scheme).environment(\.dynamicTypeSize, style.fontScale > 1.1 ? .xxLarge : style.fontScale > 1.04 ? .xLarge : style.fontScale < 0.9 ? .small : style.fontScale < 0.96 ? .medium : .large).environment(\.controlSize, style.density < 0.9 ? .small : style.density > 1.1 ? .large : .regular).tint(style.accent) } }
}

struct Track: Identifiable, Equatable {
    let id: String; let title: String; let artist: String; let album: String; let symbol: String; let colors: [Color]; let duration: Double
    var artworkURL: String? = nil
    static func == (lhs: Track, rhs: Track) -> Bool { lhs.id == rhs.id }
}
let tracks = [
    Track(id: "demo-1",title: "Midnight City",artist: "M83",album: "Hurry Up, We're Dreaming",symbol: "moon.stars.fill",colors: [Color(hex:0x7957D5),Color(hex:0xDF72CF)],duration:244),
    Track(id: "demo-2",title: "Sunset Lover",artist: "Petit Biscuit",album: "Presence",symbol: "sun.horizon.fill",colors: [Color(hex:0xF97364),Color(hex:0xFDBA74)],duration:210),
    Track(id: "demo-3",title: "Space Song",artist: "Beach House",album: "Depression Cherry",symbol: "sparkles",colors: [Color(hex:0x334155),Color(hex:0xA78BFA)],duration:320),
    Track(id: "demo-4",title: "Sweet Disposition",artist: "The Temper Trap",album: "Conditions",symbol: "wind",colors: [Color(hex:0x0F766E),Color(hex:0x6EE7B7)],duration:230),
    Track(id: "demo-5",title: "Instant Crush",artist: "Daft Punk",album: "Random Access Memories",symbol: "waveform",colors: [Color(hex:0x1D4ED8),Color(hex:0x67E8F9)],duration:337),
    Track(id: "demo-6",title: "The Less I Know the Better",artist: "Tame Impala",album: "Currents",symbol: "circle.hexagongrid.fill",colors: [Color(hex:0xDB2777),Color(hex:0xF9A8D4)],duration:216)
]

@MainActor final class PlayerStore: ObservableObject {
    @Published var current=Track(id:"empty",title:"Nothing playing",artist:"Choose a song",album:"",symbol:"music.note",colors:[Color(hex:0x344054),Color(hex:0x667085)],duration:0)
    @Published var queue:[Track]=[]; @Published var playing=false; @Published var progress=0.0; @Published var duration=0.0
    @Published var liked=false; @Published var shuffle=false; @Published var repeatMode=0; @Published var error:String?
    private let bridge=QuintalystMusicBridge(); private var player:AVPlayer?; private var observer:Any?
    init(){try? AVAudioSession.sharedInstance().setCategory(.playback,mode:.default,options:[]);configureRemoteCommands()}
    private func configureRemoteCommands(){
        let commands=MPRemoteCommandCenter.shared()
        commands.playCommand.addTarget{[weak self] _ in Task{@MainActor in self?.resume()};return .success}
        commands.pauseCommand.addTarget{[weak self] _ in Task{@MainActor in self?.pause()};return .success}
        commands.togglePlayPauseCommand.addTarget{[weak self] _ in Task{@MainActor in self?.toggle()};return .success}
        commands.nextTrackCommand.addTarget{[weak self] _ in Task{@MainActor in self?.step(1)};return .success}
        commands.previousTrackCommand.addTarget{[weak self] _ in Task{@MainActor in self?.step(-1)};return .success}
        commands.changePlaybackPositionCommand.addTarget{[weak self] event in guard let change=event as? MPChangePlaybackPositionCommandEvent else{return .commandFailed};Task{@MainActor in guard let self,self.duration>0 else{return};self.seek(to:change.positionTime/self.duration)};return .success}
    }
    private func updateNowPlaying(){MPNowPlayingInfoCenter.default().nowPlayingInfo=[MPMediaItemPropertyTitle:current.title,MPMediaItemPropertyArtist:current.artist,MPMediaItemPropertyPlaybackDuration:duration,MPNowPlayingInfoPropertyElapsedPlaybackTime:duration*progress,MPNowPlayingInfoPropertyPlaybackRate:playing ? 1.0:0.0]}
    func play(_ track:Track, from list:[Track]?=nil){if let list,!list.isEmpty{queue=list}else if queue.isEmpty{queue=[track]};current=track;playing=true;progress=0;duration=track.duration;error=nil;updateNowPlaying();Task{await loadAndPlay(track)}}
    private func loadAndPlay(_ track:Track) async {
        do {
            let payload=try await bridge.audioStreamJson(videoId:track.id)
            guard let data=payload.data(using:.utf8),let object=try JSONSerialization.jsonObject(with:data) as? [String:Any],let raw=object["url"] as? String,let url=URL(string:raw) else{throw URLError(.cannotParseResponse)}
            let headers=object["headers"] as? [String:String] ?? [:];let asset=AVURLAsset(url:url,options:["AVURLAssetHTTPHeaderFieldsKey":headers]);let item=AVPlayerItem(asset:asset);let nextPlayer=AVPlayer(playerItem:item);player=nextPlayer
            if let observer{nextPlayer.removeTimeObserver(observer);self.observer=nil}
            observer=nextPlayer.addPeriodicTimeObserver(forInterval:CMTime(seconds:0.5,preferredTimescale:600),queue:.main){[weak self] time in Task{@MainActor in guard let self else{return};let d=CMTimeGetSeconds(item.duration);if d.isFinite && d>0{self.duration=d};let t=CMTimeGetSeconds(time);if t.isFinite && self.duration>0{self.progress=min(1,max(0,t/self.duration))};self.updateNowPlaying()}}
            try? AVAudioSession.sharedInstance().setActive(true);nextPlayer.play();playing=true;updateNowPlaying()
        } catch {self.error=error.localizedDescription;playing=false;updateNowPlaying()}
    }
    func resume(){player?.play();playing=true;updateNowPlaying()}
    func pause(){player?.pause();playing=false;updateNowPlaying()}
    func toggle(){playing ? pause():resume()}
    func seek(to value:Double){progress=value;guard duration>0 else{return};player?.seek(to:CMTime(seconds:duration*value,preferredTimescale:600));updateNowPlaying()}
    func step(_ amount:Int){guard !queue.isEmpty else{return};let i=queue.firstIndex(where:{track in track.id==current.id}) ?? 0;play(queue[(i+amount+queue.count)%queue.count],from:queue)}
}
@MainActor final class StyleStore: ObservableObject {
    @Published var preset:String {didSet{save()}}; @Published var mode:String {didSet{save()}}
    @Published var colorHex:String {didSet{save()}}; @Published var radius:Double {didSet{save()}}
    @Published var density:Double {didSet{save()}}; @Published var fontScale:Double {didSet{save()}}
    @Published var glass:Bool {didSet{save()}}; @Published var dynamic:Bool {didSet{save()}}
    @Published var black:Bool {didSet{save()}}; @Published var art:Bool {didSet{save()}}
    @Published var mini:String {didSet{save()}}; @Published var player:String {didSet{save()}}; @Published var tabs:String {didSet{save()}}
    private var loading=true
    init() { let d=UserDefaults.standard.dictionary(forKey:"style") ?? [:]
        preset=d["preset"] as? String ?? "Apple Music"; mode=d["mode"] as? String ?? "System"; colorHex=d["color"] as? String ?? "#FA2D55"
        radius=d["radius"] as? Double ?? 20; density=d["density"] as? Double ?? 1; fontScale=d["font"] as? Double ?? 1
        glass=d["glass"] as? Bool ?? true; dynamic=d["dynamic"] as? Bool ?? true; black=d["black"] as? Bool ?? false; art=d["art"] as? Bool ?? true
        mini=d["mini"] as? String ?? "Glass"; player=d["player"] as? String ?? "Artwork glow"; tabs=d["tabs"] as? String ?? "Floating"; loading=false
    }
    var accent:Color { Color(hexString:colorHex) }; var scheme:ColorScheme? { mode == "Light" ? .light : mode == "Dark" ? .dark : nil }
    var bg:Color { black ? .black : Color(uiColor:.systemBackground) }
    func apply(_ name:String) { preset=name; colorHex=["Midnight":"#8B7CFF","Sunset":"#FF7B54","Forest":"#2EAD75","Rose":"#EF4F8B"][name] ?? "#FA2D55" }
    private func save() { guard !loading else{return}; UserDefaults.standard.set(["preset":preset,"mode":mode,"color":colorHex,"radius":radius,"density":density,"font":fontScale,"glass":glass,"dynamic":dynamic,"black":black,"art":art,"mini":mini,"player":player,"tabs":tabs],forKey:"style") }
}

struct MainView: View {
    @EnvironmentObject var style:StyleStore; @EnvironmentObject var audio:PlayerStore
    @State var tab=0; @State var showPlayer=false
    var body: some View { ZStack(alignment:.bottom) { style.bg.ignoresSafeArea(); Group {
        if tab==0 { HomeView() } else if tab==1 { SearchView() } else if tab==2 { LibraryView() } else { SettingsView() }
    }.safeAreaInset(edge:.bottom,spacing:0) { VStack(spacing:8) { if audio.current.id != "empty" { MiniPlayer {showPlayer=true} }; TabBar(selection:$tab) }.padding(.horizontal,14).padding(.bottom,5).background(.ultraThinMaterial) } }
        .sheet(isPresented:$showPlayer) { PlayerView().presentationDragIndicator(.visible) }
    }
}

struct HomeView: View {
    @EnvironmentObject var style:StyleStore; @EnvironmentObject var audio:PlayerStore; @EnvironmentObject var catalog:CatalogStore
    let columns=[GridItem(.flexible()),GridItem(.flexible())]
    var body:some View { ScrollView { VStack(alignment:.leading,spacing:24) {
        HStack { VStack(alignment:.leading,spacing:3) { Text(Date.now.formatted(.dateTime.weekday(.wide).month(.wide).day()).uppercased()).font(.caption.bold()).tracking(1).foregroundStyle(.secondary); Text("Listen now").font(.system(size:34,weight:.bold,design:.rounded)) }; Spacer(); Image(systemName:"person.crop.circle.fill").font(.system(size:38)).foregroundStyle(style.accent.gradient) }
        VStack(alignment:.leading,spacing:13) { SectionTitle("Made for you"); LazyVGrid(columns:columns,spacing:14) { ForEach(Array(catalog.homeItems.prefix(4))) { t in Button {audio.play(t,from:catalog.homeItems)} label: { VStack(alignment:.leading,spacing:7) { Artwork(track:t,radius:style.radius,size:0).aspectRatio(1,contentMode:.fit); Text(t.title).font(.subheadline.bold()).lineLimit(1).foregroundStyle(.primary); Text(t.artist).font(.caption).foregroundStyle(.secondary).lineLimit(1) } }.buttonStyle(.plain) } } }
        VStack(alignment:.leading,spacing:12) { SectionTitle("Recently played"); ForEach(catalog.homeItems) { t in TrackRow(track:t) {audio.play(t,from:catalog.homeItems)} } }
        if let error=catalog.errorMessage { Text(error).font(.footnote).foregroundStyle(.secondary) }
    }.padding(20).padding(.bottom,25) }.task { if catalog.homeItems.isEmpty { await catalog.loadHome() } } }
}
struct SearchView: View {
    @EnvironmentObject var audio:PlayerStore; @EnvironmentObject var catalog:CatalogStore; @State var query=""
    var body:some View { ScrollView { VStack(alignment:.leading,spacing:20) {
        Text("Search").font(.system(size:34,weight:.bold,design:.rounded)).padding(.top,15)
        HStack { Image(systemName:"magnifyingglass").foregroundStyle(.secondary); TextField("Artists, songs, albums and more",text:$query) }.padding(13).background(.secondary.opacity(0.1),in:RoundedRectangle(cornerRadius:14))
        if query.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty {
            Text("Browse categories").font(.title3.bold())
            LazyVGrid(columns:[GridItem(.flexible()),GridItem(.flexible())]) { ForEach(["Made for you","New releases","Pop","Chill","Focus","Electronic"],id:\.self) { name in Text(name).font(.headline.bold()).foregroundStyle(.white).frame(maxWidth:.infinity,minHeight:105,alignment:.bottomLeading).padding(14).background(LinearGradient(colors:[Color.accentColor,Color.purple.opacity(0.65)],startPoint:.topLeading,endPoint:.bottomTrailing),in:RoundedRectangle(cornerRadius:18)) } }
        } else if catalog.searchItems.isEmpty {
            if let error=catalog.errorMessage { Text(error).font(.footnote).foregroundStyle(.secondary) } else { ProgressView().frame(maxWidth:.infinity).padding(.top,20) }
        } else { ForEach(catalog.searchItems) { t in TrackRow(track:t){audio.play(t,from:catalog.searchItems)} } }
    }.padding(.horizontal,20) }.task(id:query) {
        let term=query.trimmingCharacters(in:.whitespacesAndNewlines)
        guard !term.isEmpty else { catalog.searchItems=[]; catalog.errorMessage=nil; return }
        try? await Task.sleep(for:.milliseconds(350))
        guard !Task.isCancelled else{return}
        await catalog.search(term)
    } }
}
struct LibraryView:View {
    @EnvironmentObject var style:StyleStore; @EnvironmentObject var audio:PlayerStore; @EnvironmentObject var catalog:CatalogStore; @State var filter="Playlists"
    var body:some View { ScrollView { VStack(alignment:.leading,spacing:18) {
        HStack { Text("Library").font(.system(size:34,weight:.bold,design:.rounded)); Spacer(); Image(systemName:"plus").foregroundStyle(style.accent) }.padding(.top,15)
        HStack { ForEach(["Playlists","Songs","Albums","Artists"],id:\.self){x in Button(x){filter=x}.font(.caption.bold()).padding(.horizontal,13).padding(.vertical,8).background(filter==x ? style.accent : Color.secondary.opacity(0.1),in:Capsule()).foregroundStyle(filter==x ? Color.white : Color.primary)} }
        ForEach(catalog.libraryItems){t in TrackRow(track:t){audio.play(t,from:catalog.libraryItems)}}
        if let error=catalog.errorMessage { Text(error).font(.footnote).foregroundStyle(.secondary) }
    }.padding(.horizontal,20) }.task { if catalog.libraryItems.isEmpty { await catalog.loadLibrary() } } }
}
struct PlayerView:View {
    @EnvironmentObject var style:StyleStore; @EnvironmentObject var audio:PlayerStore; @Environment(\.dismiss) var dismiss; @State var lyrics=false
    var body:some View { ZStack { LinearGradient(colors:audio.current.colors.map{$0.opacity(style.dynamic ? 0.58 : 0.16)}+[style.bg],startPoint:.topLeading,endPoint:.bottomTrailing).ignoresSafeArea()
        VStack(spacing:22) { HStack {Image(systemName:"chevron.down").onTapGesture{dismiss()}; Spacer(); VStack(spacing:2){Text("PLAYING FROM").font(.caption2.bold()).tracking(1).foregroundStyle(.secondary);Text("Your Mix").font(.subheadline.bold())};Spacer();Image(systemName:"ellipsis")}.padding(.top,10)
        if lyrics { Spacer(); Image(systemName:"quote.opening").font(.largeTitle).foregroundStyle(style.accent); Text("Waiting in the summer rain\nFor the night to take us home").font(.title2.bold()).multilineTextAlignment(.center); Text("Lyrics preview").font(.caption).foregroundStyle(.secondary); Spacer() }
        else { if style.art && style.player != "Minimal" { Artwork(track:audio.current,radius:style.radius,size:0).aspectRatio(1,contentMode:.fit).padding(.horizontal,18).shadow(color:audio.current.colors[0].opacity(0.3),radius:28,y:15) }
        HStack { VStack(alignment:.leading,spacing:4){Text(audio.current.title).font(.title2.bold()).lineLimit(1);Text(audio.current.artist).font(.title3).foregroundStyle(.secondary)};Spacer();Button{audio.liked.toggle()}{Image(systemName:audio.liked ? "heart.fill":"heart").font(.title2).foregroundStyle(audio.liked ? style.accent:Color.secondary)} }
        VStack(spacing:2){Slider(value:Binding(get:{audio.progress},set:{audio.seek(to:$0)})).tint(style.accent);HStack{Text(clock(audio.duration*audio.progress));Spacer();Text("−"+clock(audio.duration*(1-audio.progress)))}.font(.caption2.monospacedDigit()).foregroundStyle(.secondary)}
        HStack {Button{audio.shuffle.toggle()}{Image(systemName:"shuffle").foregroundStyle(audio.shuffle ? style.accent:Color.secondary)};Spacer();Button{audio.step(-1)}{Image(systemName:"backward.end.fill")};Spacer();Button{audio.toggle()}{Image(systemName:audio.playing ? "pause.fill":"play.fill").font(.title2.bold()).foregroundStyle(.black).frame(width:66,height:66).background(.white,in:Circle())};Spacer();Button{audio.step(1)}{Image(systemName:"forward.end.fill")};Spacer();Button{audio.repeatMode=(audio.repeatMode+1)%3}{Image(systemName:audio.repeatMode>0 ? "repeat.1":"repeat").foregroundStyle(audio.repeatMode>0 ? style.accent:Color.secondary)} }.font(.title3).foregroundStyle(.primary)
        if let error=audio.error { Text(error).font(.footnote).foregroundStyle(.red).lineLimit(2) }; HStack {Label("AirPlay",systemImage:"airplayaudio");Spacer();Image(systemName:"list.bullet");Image(systemName:"quote.bubble").onTapGesture{lyrics=true}}.font(.subheadline).foregroundStyle(.secondary) }
        HStack {Button("Now Playing"){lyrics=false};Button("Lyrics"){lyrics=true}}.font(.caption.bold()).foregroundStyle(style.accent).padding(.bottom,8)
        }.padding(.horizontal,26)
    } }
}

struct SettingsView:View {
    @EnvironmentObject var style:StyleStore; @EnvironmentObject var catalog:CatalogStore
    let presets:[(String,Color)]=[("Apple Music",Color(hex:0xFA2D55)),("Midnight",Color(hex:0x8B7CFF)),("Sunset",Color(hex:0xFF7B54)),("Forest",Color(hex:0x2EAD75)),("Rose",Color(hex:0xEF4F8B))]
    var body:some View { NavigationStack { Form {
        Section {HStack(spacing:13){Image(systemName:"waveform").font(.title2.bold()).foregroundStyle(.white).frame(width:56,height:56).background(style.accent.gradient,in:RoundedRectangle(cornerRadius:16));VStack(alignment:.leading){Text("Quintalyst").font(.headline);Text("Make it yours").font(.subheadline).foregroundStyle(.secondary)}}.padding(.vertical,4)}
        Section("Your aesthetic") {Text("Choose a look").font(.subheadline.bold());ScrollView(.horizontal,showsIndicators:false){HStack(spacing:9){ForEach(presets.indices,id:\.self){idx in let name=presets[idx].0;let color=presets[idx].1;Button{style.apply(name)}label:{VStack(spacing:6){Circle().fill(color.gradient).frame(width:39,height:39).overlay{if style.preset==name{Image(systemName:"checkmark").font(.caption.bold()).foregroundStyle(.white)}};Text(name).font(.caption2).foregroundStyle(.primary)}.frame(width:72,height:78).background(style.preset==name ? color.opacity(0.12):.clear,in:RoundedRectangle(cornerRadius:13))}.buttonStyle(.plain)}}}
            Picker("Appearance",selection:$style.mode){Text("System").tag("System");Text("Light").tag("Light");Text("Dark").tag("Dark")};ColorPicker("Accent color",selection:Binding(get:{style.accent},set:{style.colorHex=$0.hexString}));Toggle("Dynamic artwork backgrounds",isOn:$style.dynamic);Toggle("Pure black in dark mode",isOn:$style.black) }
        Section("Customize every detail") {CustomSlider(title:"Artwork corner radius",value:$style.radius,range:0...30,suffix:" pt");CustomSlider(title:"Screen density",value:$style.density,range:0.8...1.2,multiplier:100);CustomSlider(title:"Text size",value:$style.fontScale,range:0.85...1.2,multiplier:100);Toggle("Translucent glass surfaces",isOn:$style.glass);Toggle("Show album artwork in player",isOn:$style.art)
            Picker("Mini player",selection:$style.mini){ForEach(["Glass","Solid","Artwork tint","Compact"],id:\.self){Text($0)}};Picker("Full player",selection:$style.player){ForEach(["Artwork glow","Minimal","Glass","Classic"],id:\.self){Text($0)}};Picker("Tab bar",selection:$style.tabs){ForEach(["Floating","Inset","Compact"],id:\.self){Text($0)}} }
        Section("Account") {NavigationLink(catalog.isSignedIn ? "YouTube Music connected" : "Connect YouTube Music",destination:SignInScreen());if catalog.isSignedIn{Button("Sign out",role:.destructive){catalog.signOut()}}}
                Section("Playback") {NavigationLink("Playback settings",destination:PlaybackSettingsView());NavigationLink("Preview your style",destination:StylePreviewView())}
        Section("About") {HStack{Text("Version");Spacer();Text("iOS Preview 0.1").foregroundStyle(.secondary)};Text("Quintalyst for iPhone").foregroundStyle(.secondary)}
    }.scrollContentBackground(.hidden).background(style.bg).navigationTitle("Settings").navigationBarTitleDisplayMode(.large) }.tint(style.accent) }
}

struct PlaybackSettingsView:View {
    @State var autoplay=true;@State var crossfade=false;@State var silence=false;@State var normalize=true
    var body:some View {Form {Section("Listening"){Toggle("Autoplay similar music",isOn:$autoplay);Toggle("Crossfade",isOn:$crossfade);if crossfade{Slider(value:.constant(4),in:1...12)};Toggle("Skip silence",isOn:$silence);Toggle("Normalize volume",isOn:$normalize)};Section{Text("Some advanced playback effects are not implemented in the iOS version yet.").font(.footnote).foregroundStyle(.secondary)}}.navigationTitle("Playback").navigationBarTitleDisplayMode(.inline)}
}
struct StylePreviewView:View {
    @EnvironmentObject var style:StyleStore
    var body:some View {ScrollView{VStack(spacing:20){HStack{Image(systemName:"sparkles");Text("Your Quintalyst").font(.headline);Spacer();Image(systemName:"ellipsis")}.foregroundStyle(style.accent);Artwork(track:tracks[0],radius:style.radius,size:0).aspectRatio(1,contentMode:.fit);HStack{VStack(alignment:.leading){Text("Midnight City").font(.title2.bold());Text("M83").foregroundStyle(.secondary)};Spacer();Image(systemName:"heart.fill").foregroundStyle(style.accent)};HStack{Image(systemName:"shuffle");Spacer();Image(systemName:"backward.end.fill");Spacer();Image(systemName:"play.fill").font(.title).foregroundStyle(.black).frame(width:58,height:58).background(.white,in:Circle());Spacer();Image(systemName:"forward.end.fill");Spacer();Image(systemName:"repeat")};Text("Preview updates as you adjust appearance settings.").font(.footnote).foregroundStyle(.secondary)}.padding(22)}.navigationTitle("Style preview").navigationBarTitleDisplayMode(.inline).background(style.bg)}
}

struct MiniPlayer:View {
    @EnvironmentObject var style:StyleStore;@EnvironmentObject var audio:PlayerStore;let action:()->Void
    var body:some View {HStack(spacing:10){if style.art && style.mini != "Compact"{Artwork(track:audio.current,radius:min(style.radius,11),size:42)};VStack(alignment:.leading,spacing:3){Text(audio.current.title).font(.subheadline.bold()).lineLimit(1);Text(audio.current.artist).font(.caption).foregroundStyle(.secondary).lineLimit(1)};Spacer(minLength:3);Button{audio.toggle()}{Image(systemName:audio.playing ? "pause.fill":"play.fill").foregroundStyle(.primary).frame(width:35,height:40)};Button{audio.step(1)}{Image(systemName:"forward.end.fill").foregroundStyle(.primary).frame(width:30,height:40)}}.padding(style.mini == "Compact" ? 5 : 8).background(style.mini == "Glass" ? AnyShapeStyle(.ultraThinMaterial) : AnyShapeStyle(style.mini == "Artwork tint" ? audio.current.colors[0].opacity(0.28) : Color(uiColor:.secondarySystemBackground)),in:RoundedRectangle(cornerRadius:17)).overlay(alignment:.bottomLeading){GeometryReader{g in Capsule().fill(style.accent).frame(width:g.size.width*audio.progress,height:2)}.padding(.horizontal,12)}.contentShape(Rectangle()).onTapGesture(perform:action)}
}
struct TabBar:View {
    @EnvironmentObject var style:StyleStore;@Binding var selection:Int
    let items:[(String,String)]=[("Listen Now","play.circle"),("Search","magnifyingglass"),("Library","square.stack"),("Settings","slider.horizontal.3")]
    var body:some View {HStack(spacing:0){ForEach(0..<items.count,id:\.self){i in Button{selection=i}label:{VStack(spacing:4){Image(systemName:items[i].1).font(.system(size:style.tabs=="Compact" ? 16:19,weight:.medium));Text(items[i].0).font(.system(size:style.tabs=="Compact" ? 9:10,weight:.medium))}.foregroundStyle(selection==i ? style.accent:Color.secondary).frame(maxWidth:.infinity).padding(.vertical,7)}.buttonStyle(.plain)}}.background(style.tabs == "Floating" ? AnyShapeStyle(.ultraThinMaterial) : AnyShapeStyle(Color(uiColor:.secondarySystemBackground)),in:RoundedRectangle(cornerRadius:style.tabs=="Floating" ? 20:12))}
}
struct Artwork:View {
    let track:Track;let radius:Double;let size:CGFloat
    var body:some View {ZStack{LinearGradient(colors:track.colors,startPoint:.topLeading,endPoint:.bottomTrailing);if let raw=track.artworkURL,let url=URL(string:raw){AsyncImage(url:url) { image in image.resizable().scaledToFill() } placeholder: { Color.clear }.frame(maxWidth:.infinity,maxHeight:.infinity).clipped()}else{Circle().fill(.white.opacity(0.13)).frame(width:size>0 ? size*0.75:240).offset(x:size>0 ? size*0.3:100,y:size>0 ? -size*0.35:-100);Image(systemName:track.symbol).font(.system(size:size>0 ? size*0.36:76,weight:.light)).foregroundStyle(.white.opacity(0.9))}}.frame(width:size>0 ? size:nil,height:size>0 ? size:nil).clipShape(RoundedRectangle(cornerRadius:radius,style:.continuous))}
}
struct TrackRow:View {
    @EnvironmentObject var style:StyleStore;let track:Track;let action:()->Void
    var body:some View {Button(action:action){HStack(spacing:13){Artwork(track:track,radius:min(style.radius,12),size:56);VStack(alignment:.leading,spacing:4){Text(track.title).font(.subheadline.bold()).lineLimit(1);Text(track.artist).font(.caption).foregroundStyle(.secondary).lineLimit(1)};Spacer(minLength:5);Image(systemName:"ellipsis").foregroundStyle(.secondary).padding(8)}.contentShape(Rectangle())}.buttonStyle(.plain).foregroundStyle(.primary)}
}
struct SectionTitle:View {let title:String;init(_ title:String){self.title=title};var body:some View{Text(title).font(.title2.bold())}}
struct CustomSlider:View {
    let title:String;@Binding var value:Double;let range:ClosedRange<Double>;var suffix="";var multiplier=1.0
    var body:some View {VStack(spacing:3){HStack{Text(title);Spacer();Text("\(Int(value*multiplier))\(suffix)").foregroundStyle(.secondary).monospacedDigit()};Slider(value:$value,in:range)}.padding(.vertical,3)}
}
func clock(_ v:Double)->String{let n=Int(max(0,v));return String(format:"%d:%02d",n/60,n%60)}
extension Color {
    init(hex:UInt){self.init(.sRGB,red:Double((hex>>16)&255)/255,green:Double((hex>>8)&255)/255,blue:Double(hex&255)/255,opacity:1)}
    init(hexString:String){let s=hexString.trimmingCharacters(in:CharacterSet.alphanumerics.inverted);let v=UInt64(s,radix:16) ?? 0xFA2D55;self.init(.sRGB,red:Double((v>>16)&255)/255,green:Double((v>>8)&255)/255,blue:Double(v&255)/255,opacity:1)}
    var hexString:String{guard let c=UIColor(self).cgColor.components,c.count>=3 else{return "#FA2D55"};return String(format:"#%02X%02X%02X",Int(c[0]*255),Int(c[1]*255),Int(c[2]*255))}
}






@MainActor final class CatalogStore: ObservableObject {
    @Published var homeItems:[Track]=[]; @Published var searchItems:[Track]=[]; @Published var libraryItems:[Track]=[]
    @Published var errorMessage:String?; @Published var isSignedIn=false
    private let bridge=QuintalystMusicBridge()
    init(){if let session=SessionVault.load(){isSignedIn=true;bridge.setSession(cookie:session.cookie,visitorData:session.visitorData,dataSyncId:session.dataSyncId,authUser:session.authUser)}}
    func signIn(_ session:YouTubeSession){SessionVault.save(session);bridge.setSession(cookie:session.cookie,visitorData:session.visitorData,dataSyncId:session.dataSyncId,authUser:session.authUser);isSignedIn=true;Task{await loadHome();await loadLibrary()}}
    func signOut(){SessionVault.remove();bridge.clearSession();isSignedIn=false;homeItems=[];libraryItems=[];searchItems=[]}
    func loadHome() async {do{homeItems=Self.parse(try await bridge.homeJson());errorMessage=nil}catch{errorMessage=error.localizedDescription}}
    func loadLibrary() async {do{libraryItems=Self.parse(try await bridge.libraryJson());errorMessage=nil}catch{errorMessage=error.localizedDescription}}
    func search(_ query:String) async {do{searchItems=Self.parse(try await bridge.searchJson(query:query));errorMessage=nil}catch{searchItems=[];errorMessage=error.localizedDescription}}
    private static func parse(_ text:String)->[Track] {
        guard let data=text.data(using:.utf8),let root=try? JSONSerialization.jsonObject(with:data) else{return []}
        var renderers=[[String:Any]]()
        func visit(_ value:Any){if let object=value as? [String:Any]{if let item=object["musicResponsiveListItemRenderer"] as? [String:Any]{renderers.append(item)};for child in object.values{visit(child)}}else if let array=value as? [Any]{for child in array{visit(child)}}}
        visit(root)
        let palettes:[[Color]]=[[Color(hex:0x7957D5),Color(hex:0xDF72CF)],[Color(hex:0xF97364),Color(hex:0xFDBA74)],[Color(hex:0x0F766E),Color(hex:0x6EE7B7)],[Color(hex:0x1D4ED8),Color(hex:0x67E8F9)]]
        var seen=Set<String>()
        return renderers.compactMap{item in
            let columns=(item["flexColumns"] as? [[String:Any]] ?? []).map{column -> String in let renderer=column["musicResponsiveListItemFlexColumnRenderer"] as? [String:Any] ?? [:];let text=renderer["text"] as? [String:Any] ?? [:];let runs=text["runs"] as? [[String:Any]] ?? [];return runs.compactMap{$0["text"] as? String}.joined()}
            guard let title=columns.first,!title.isEmpty else{return nil}
            let playlist=item["playlistItemData"] as? [String:Any];let endpoint=item["navigationEndpoint"] as? [String:Any];let watch=endpoint?["watchEndpoint"] as? [String:Any]
            guard let id=(playlist?["videoId"] as? String) ?? (watch?["videoId"] as? String),seen.insert(id).inserted else{return nil}
            let thumbnail=item["thumbnail"] as? [String:Any];let renderer=thumbnail?["musicThumbnailRenderer"] as? [String:Any];let container=renderer?["thumbnail"] as? [String:Any];let thumbs=container?["thumbnails"] as? [[String:Any]] ?? [];let artwork=thumbs.last?["url"] as? String
            let fixed=item["fixedColumns"] as? [[String:Any]] ?? [];let durationText=fixed.compactMap{column -> String? in let r=column["musicResponsiveListItemFlexColumnRenderer"] as? [String:Any];let t=r?["text"] as? [String:Any];return (t?["runs"] as? [[String:Any]])?.compactMap{$0["text"] as? String}.joined()}.first ?? ""
            let seconds=durationText.split(separator:":").compactMap{Double($0)}.reduce(0.0){$0*60+$1};let paletteIndex=abs(id.hashValue)%palettes.count
            return Track(id:id,title:title,artist:columns.dropFirst().first ?? "",album:"",symbol:"music.note",colors:palettes[paletteIndex],duration:seconds,artworkURL:artwork)
        }
    }
}

struct YouTubeSession:Codable {let cookie:String;let visitorData:String;let dataSyncId:String;let authUser:String}
private enum SessionVault {
    private static let service="com.quintalyst.ios.youtube-session"
    static func save(_ session:YouTubeSession){remove();guard let data=try? JSONEncoder().encode(session) else{return};let query:[String:Any]=[kSecClass as String:kSecClassGenericPassword,kSecAttrService as String:service,kSecAttrAccount as String:"music",kSecValueData as String:data];SecItemAdd(query as CFDictionary,nil)}
    static func load()->YouTubeSession?{let query:[String:Any]=[kSecClass as String:kSecClassGenericPassword,kSecAttrService as String:service,kSecAttrAccount as String:"music",kSecReturnData as String:true,kSecMatchLimit as String:kSecMatchLimitOne];var result:CFTypeRef?;guard SecItemCopyMatching(query as CFDictionary,&result)==errSecSuccess,let data=result as? Data else{return nil};return try? JSONDecoder().decode(YouTubeSession.self,from:data)}
    static func remove(){let query:[String:Any]=[kSecClass as String:kSecClassGenericPassword,kSecAttrService as String:service,kSecAttrAccount as String:"music"];SecItemDelete(query as CFDictionary)}
}

struct YouTubeLoginWebView:UIViewRepresentable {
    let onSession:(YouTubeSession)->Void
    func makeCoordinator()->Coordinator{Coordinator(onSession:onSession)}
    func makeUIView(context:Context)->WKWebView{let config=WKWebViewConfiguration();config.websiteDataStore = .default();let view=WKWebView(frame:.zero,configuration:config);view.navigationDelegate=context.coordinator;view.load(URLRequest(url:URL(string:"https://music.youtube.com")!));return view}
    func updateUIView(_ uiView:WKWebView,context:Context){}
    final class Coordinator:NSObject,WKNavigationDelegate {
        let onSession:(YouTubeSession)->Void;var delivered=false
        init(onSession:@escaping(YouTubeSession)->Void){self.onSession=onSession}
        func webView(_ webView:WKWebView,didFinish navigation:WKNavigation!){guard !delivered else{return};let script="""(()=>{const c=window.yt&&window.yt.config_||{};return JSON.stringify({visitorData:c.VISITOR_DATA||'',dataSyncId:String(c.DATASYNC_ID||'').split('||')[0],authUser:String(c.SESSION_INDEX||'0')})})()""";webView.evaluateJavaScript(script){[weak self] result,_ in guard let self,let text=result as? String,let data=text.data(using:.utf8),let auth=try? JSONSerialization.jsonObject(with:data) as? [String:String] else{return};webView.configuration.websiteDataStore.httpCookieStore.getAllCookies{cookies in let cookie=cookies.filter{$0.domain.contains("youtube.com")}.map{"\($0.name)=\($0.value)"}.joined(separator:"; ");guard cookie.contains("SAPISID="),let visitor=auth["visitorData"],!visitor.isEmpty else{return};let session=YouTubeSession(cookie:cookie,visitorData:visitor,dataSyncId:auth["dataSyncId"] ?? "",authUser:auth["authUser"] ?? "0");self.delivered=true;DispatchQueue.main.async{self.onSession(session)}}}}
    }
}

struct SignInScreen:View {
    @EnvironmentObject var catalog:CatalogStore;@Environment(\.dismiss) var dismiss
    var body:some View{VStack(spacing:10){Text("Sign in to YouTube Music").font(.headline).padding(.top,10);Text("Your session is stored securely on this device.").font(.footnote).foregroundStyle(.secondary);YouTubeLoginWebView{session in catalog.signIn(session);dismiss()}.ignoresSafeArea(edges:.bottom)}.navigationTitle("Account").navigationBarTitleDisplayMode(.inline)}
}


