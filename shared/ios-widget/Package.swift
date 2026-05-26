// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "TranslationSDKUI",
    platforms: [
        .iOS(.v15),
        .macOS(.v14)
    ],
    products: [
        .library(
            name: "TranslationSDKUI",
            targets: ["TranslationSDKUI"]
        )
    ],
    targets: [
        .target(
            name: "TranslationSDKUI"
        )
    ]
)
