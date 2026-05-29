import ComposeSampleUI
import SwiftUI
import TranslationSDK
import TranslationSDKUI
import UIKit

struct ContentView: View {
    var body: some View {
        TabView {
            NativeWidgetSample()
                .tabItem {
                    Label("SwiftUI", systemImage: "swift")
                }

            ComposeWidgetSample()
                .tabItem {
                    Label("Compose", systemImage: "square.grid.2x2")
                }
        }
    }
}

private struct NativeWidgetSample: View {
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
        TranslationSDK.companion.instance.translate(text: text, targetLanguage: targetLanguage) { result, _ in
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
