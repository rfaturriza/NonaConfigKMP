// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://api.github.com/repos/rfaturriza/NonaConfigKMP/releases/assets/550689539.zip"
let remoteKotlinChecksum = "75121bb68468c1e36ccc6febe42342aa7a5a1f8f76c6e84449aafbeaa0f8b851"
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