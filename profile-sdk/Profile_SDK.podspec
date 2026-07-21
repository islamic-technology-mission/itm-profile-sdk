Pod::Spec.new do |spec|
    spec.name                     = 'Islam360SDK'
    spec.version                  = '1.0.0'
    spec.homepage                 = 'https://github.com/islamic-technology-mission/Islam360_CocoaPods_IOS_SDK.git'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Profile-SDK Kotlin Multiplatform SDK'
    spec.vendored_frameworks      = 'build/cocoapods/framework/Profile_SDK.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '13.0'
    if !Dir.exist?('build/cocoapods/framework/Profile_SDK.framework') || Dir.empty?('build/cocoapods/framework/Profile_SDK.framework')
        raise "
        Kotlin framework 'Profile_SDK' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:
            ./gradlew :profile-sdk:generateDummyFramework
        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':profile-sdk',
        'PRODUCT_MODULE_NAME' => 'Profile_SDK',
    }
    
end
