//  ProfileSDKServiceAsync.swift
//  Bridges KMP ISDKClient callbacks to Swift closures (no async/await).

import Foundation
import Profile_SDK

struct AppProfileAsync {
    let label: String
    let userId: String
}

enum SDKErrorAsync: Error {
    case notInitialized
    case message(String)
}

final class ProfileSDKServiceAsync {

    static let internalKey = "6bebaa8645a54b27b858d74cbf5a1aac9f56b4945aa2f7ab85df540c5b"

    static let profiles: [AppProfileAsync] = [
        AppProfileAsync(label: "P1", userId: "mMCb2xH89eUWqnJPOkV0WfGf2XO"),
        AppProfileAsync(label: "P2", userId: "TbyvkxSKuBSNSRUdwv9uDfhbN12"),
        AppProfileAsync(label: "P3", userId: "xPVwfiBkHSX6qBqnufSUssJdDI2"),
        AppProfileAsync(label: "P4", userId: "wFpRlxosKPXKzjjDqwXpXVicoL2"),
    ]

    private var profileCancellable: Cancellable?
    private var screenTimeCancellable: Cancellable?

    // MARK: - Initialize + Token

    func initialize(userId: String) {
        ISDKClient.shared.initialize(userId: userId, context: NSObject())
    }

    func generateToken(completion: @escaping (String?, Error?) -> Void) {
        ISDKClient.shared.generateToken(internalKey: Self.internalKey) { result in
            if let success = result as? ResultSuccess<NSString> {
                completion(success.data! as String, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKErrorAsync.message(error.message))
            } else {
                completion(nil, SDKErrorAsync.message("Unknown token error"))
            }
        }
    }

    // MARK: - Observe Profile
    // Reverted from AsyncStream to closures

    func observeProfile(
        token: String,
        onEach: @escaping (UserProfile) -> Void,
        onError: @escaping (Error) -> Void
    ) {
        profileCancellable?.cancel()
        profileCancellable = ISDKClient.shared.observeProfile(
            token: token,
            onEach: { profile in
                onEach(profile)
            },
            onError: { throwable in
                onError(SDKErrorAsync.message(throwable.message ?? "Profile observe error"))
            }
        )
    }

    func stopObservingProfile() {
        profileCancellable?.cancel()
        profileCancellable = nil
    }

    // MARK: - Update Profile

    func updateProfile(token: String, current: UserProfile, completion: @escaping (UserProfile?, Error?) -> Void) {
        let currentName = current.name ?? ""
        let updatedName = currentName.hasSuffix("(Updated)")
            ? String(currentName.dropLast(" (Updated)".count))
            : currentName + " (Updated)"

        let request = UpdateProfileRequest(
            name: updatedName,
            phone: current.phone,
            gender: current.gender,
            dob: current.dob,
            visibility: current.visibility,
            location: current.location,
            umrahOptIn: current.umrahOptIn
        )

        ISDKClient.shared.updateProfile(token: token, request: request) { result in
            if let success = result as? ResultSuccess<UserProfile> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKErrorAsync.message(error.message))
            } else {
                completion(nil, SDKErrorAsync.message("Unknown update error"))
            }
        }
    }

    // MARK: - Screen Time

    func observeScreenTime(
        token: String,
        days: Int32 = 7,
        onEach: @escaping ([ScreenTimeEntry]) -> Void,
        onError: @escaping (Error) -> Void,
        onComplete: @escaping () -> Void
    ) {
        screenTimeCancellable?.cancel()
        screenTimeCancellable = ISDKClient.shared.observeScreenTime(
            token: token,
            days: days,
            onEach: { entries in
                onEach(entries as! [ScreenTimeEntry])
            },
            onError: { throwable in
                onError(SDKErrorAsync.message(throwable.message ?? "Screen time observe error"))
            },
            onComplete: {
                onComplete()
            }
        )
    }

    func stopObservingScreenTime() {
        screenTimeCancellable?.cancel()
        screenTimeCancellable = nil
    }

    func postScreenTime(token: String, completion: @escaping (Error?) -> Void) {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let today = formatter.string(from: Date())
        let request = ScreenTimeRequest(date: today, seconds: 60)

        ISDKClient.shared.postScreenTime(token: token, days: 7, request: request) { result in
            if result is ResultSuccess<AnyObject> {
                completion(nil)
            } else if let error = result as? ResultError {
                completion(SDKErrorAsync.message(error.message))
            } else {
                completion(SDKErrorAsync.message("Unknown post screen time error"))
            }
        }
    }

    // MARK: - Profile Views

    func getProfileViews(token: String, completion: @escaping (ProfileViewsData?, Error?) -> Void) {
        ISDKClient.shared.getProfileViews(token: token, cursor: nil, limit: nil) { result in
            if let success = result as? ResultSuccess<ProfileViewsData> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKErrorAsync.message(error.message))
            } else {
                completion(nil, SDKErrorAsync.message("Unknown profile views error"))
            }
        }
    }

    // MARK: - Nearby Users

    func getNearbyUsers(token: String, completion: @escaping ([NearbyUser]?, Error?) -> Void) {
        ISDKClient.shared.getNearbyUsers(token: token, lat: 24.8607, lng: 67.0011) { result in
            if let success = result as? ResultSuccess<NSArray> {
                let users = (success.data as? [NearbyUser]) ?? []
                completion(users, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKErrorAsync.message(error.message))
            } else {
                completion(nil, SDKErrorAsync.message("Unknown nearby users error"))
            }
        }
    }

    // MARK: - Cleanup

    func cancelAll() {
        stopObservingProfile()
        stopObservingScreenTime()
    }
}
