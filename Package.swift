// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "NonaConfig",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "NonaConfig",
            targets: ["NonaConfig"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "NonaConfig",
            // For local development/testing, point to the build folder
            // For remote distribution, this would be a URL to a zipped XCFramework in a GitHub Release
            path: "./sharedLogic/build/XCFrameworks/release/NonaConfig.xcframework"
        ),
    ]
)
