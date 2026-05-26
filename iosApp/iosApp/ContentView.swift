import ComposeUI
import SwiftUI
import TranslationSDK
import TranslationSDKUI
import UIKit

struct ContentView: View {
    private let sdk = TranslationSDK.Builder()
        .apiKey(key: "YOUR_GOOGLE_TRANSLATE_API_KEY")
        .defaultLanguage(lang: "de")
        .build()

    var body: some View {
        TabView {
            NativeWidgetSample(sdk: sdk)
                .tabItem {
                    Text("SwiftUI")
                }

            ComposeWidgetSample()
                .tabItem {
                    Text("Compose")
                }
        }
    }
}

private struct NativeWidgetSample: View {
    let sdk: TranslationSDK

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text("SwiftUI TranslatedText")
                    .font(.title)
                    .fontWeight(.semibold)

                TranslatedText(
                    rawText: "Welcome to the TranslatedText Widget sample app.",
                    translationRequired: true,
                    targetLanguage: "de",
                    translate: translate
                )

                TranslatedText(
                    rawText: "This text can be translated into French.",
                    translationRequired: true,
                    targetLanguage: "fr",
                    seeTranslationText: "Voir la traduction",
                    hideTranslationText: "Masquer la traduction",
                    translate: translate
                )

                TranslatedText(
                    rawText: "This text does not require translation.",
                    translate: translate
                )
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func translate(
        text: String,
        targetLanguage: String,
        completion: @escaping (String?) -> Void
    ) {
        sdk.translate(text: text, targetLanguage: targetLanguage) { result, _ in
            completion(result?.translatedText)
        }
    }
}

private struct ComposeWidgetSample: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
