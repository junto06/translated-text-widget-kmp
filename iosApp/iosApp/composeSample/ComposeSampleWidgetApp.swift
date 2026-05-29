import SwiftUI
import TranslationSDK

private final class AppSDK: ObservableObject {
    init() {
        TranslationSDK.companion.doInit(sdk:
            TranslationSDK.Builder()
                .translationApi(api: DeepSeekTranslationApi(
                    apiKey: deepseekApiKey,
                    model: "deepseek-chat"
                ))
                .build()
        )
    }

    deinit {
        TranslationSDK.companion.close()
    }
}

@main
struct ComposeSampleWidgetApp: App {
    @StateObject private var appSdk = AppSDK()

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
