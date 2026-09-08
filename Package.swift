// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://api.github.com/repos/rfaturriza/NonaConfigKMP/releases/assets/549706579.zip"
let remoteKotlinChecksum = "9c5edd4d83aeb45ce6f6ecfb26ce413c298e378f835eebc4dc8b821dce64d305"
let packageName = "NonaConfig"
// END KMMBRIDGE BLOCK

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: remoteKotlinUrl,
            checksum: remoteKotlinChecksum
        )
        ,
    ]
)