Pod::Spec.new do |spec|
    spec.name                     = 'Profile_SDK'
    spec.version                  = '1.0'
    spec.homepage                 = 'https://theislam360.com'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Profile-SDK Kotlin Multiplatform SDK'
    spec.vendored_frameworks      = 'build/cocoapods/framework/Profile_SDK.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '15.0'
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
    spec.script_phases = [
        {
            :name => 'Build Profile_SDK',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                    echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                    exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
end
