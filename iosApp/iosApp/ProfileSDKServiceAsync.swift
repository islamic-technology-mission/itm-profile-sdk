//  ProfileSDKServiceAsync.swift
//  Bridges KMP ISDKClient callbacks to Swift closures (no async/await).

import Foundation
import Islam360SDK

final class ProfileSDKServiceAsync {

    static let internalKey = "6bebaa8645a54b27b858d74cbf5a1aac9f56b4945aa2f7ab85df540c5b"

    private var profileCancellable: Cancellable?
    private var screenTimeCancellable: Cancellable?

    // MARK: - Lifecycle

    /// Must be called once at app startup.
    func setup(sandboxMode: Bool = true) {
        ISDKClient.shared.setup(sandboxMode: sandboxMode, context: NSObject())
    }

    /// Sets the current user. Requires setup() to have been called once.
    func initialize(userId: String) {
        ISDKClient.shared.initialize(userId: userId)
    }

    func logout() {
        cancelAll()
        ISDKClient.shared.logout()
    }

    func reset() {
        cancelAll()
        ISDKClient.shared.reset()
    }

    // MARK: - Token

    func generateToken(completion: @escaping (String?, Error?) -> Void) {
        ISDKClient.shared.generateToken(internalKey: Self.internalKey) { result in
            if let success = result as? ResultSuccess<NSString> {
                completion(success.data! as String, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            } else {
                completion(nil, SDKError.message("Unknown token error"))
            }
        }
    }

    // MARK: - Observe Profile

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
                onError(SDKError.message(throwable.message ?? "Profile observe error"))
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
                completion(nil, SDKError.message(error.message))
            } else {
            onEach: { entries in
                onEach(entries as! [ScreenTimeEntry])
            },
            onError: { throwable in
                onError(SDKError.message(throwable.message ?? "Screen time observe error"))
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
    completion(nil, SDKError.message("Unknown update error"))
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

    func postScreenTime(token: String, completion: @escaping (Error?) -> Void) {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let today = formatter.string(from: Date())
        let request = ScreenTimeRequest(date: today, seconds: 60)

        // Updated to match new signature: token, request, days
        ISDKClient.shared.postScreenTime(token: token, request: request, days: 7) { result in
            if result is ResultSuccess<AnyObject> {
                completion(nil)
            } else if let error = result as? ResultError {
                completion(SDKError.message(error.message))
            } else {
                completion(SDKError.message("Unknown post screen time error"))
            }
        }
    }

    // MARK: - Profile Views

    func getProfileViews(token: String, completion: @escaping (ProfileViewsData?, Error?) -> Void) {
        ISDKClient.shared.getProfileViews(token: token, cursor: nil, limit: nil) { result in
            if let success = result as? ResultSuccess<ProfileViewsData> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            } else {
                completion(nil, SDKError.message("Unknown profile views error"))
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
                completion(nil, SDKError.message(error.message))
            } else {
                completion(nil, SDKError.message("Unknown nearby users error"))
            }
        }
    }

    // MARK: - Complete Profile Data (New API)

    func getCompleteProfileData(token: String, completion: @escaping (UserProfileData?, Error?) -> Void) {
        ISDKClient.shared.getCompleteProfileData(token: token) { result in
            if let success = result as? ResultSuccess<UserProfileData> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            } else {
                completion(nil, SDKError.message("Unknown complete profile data error"))
            }
        }
    }

    // MARK: - Upsert Profile
    func upsertProfile(token: String, request: UpsertProfileRequest, completion: @escaping (UserProfile?, Error?) -> Void) {
        ISDKClient.shared.upsertProfile(token: token, request: request) { result in
            if let success = result as? ResultSuccess<UserProfile> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            }
        }
    }

    // MARK: - Standalone Subscription
    func getSubscription(token: String, completion: @escaping (Subscription?, Error?) -> Void) {
        ISDKClient.shared.getSubscription(token: token) { result in
            if let success = result as? ResultSuccess<Subscription> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            }
        }
    }

    // MARK: - Cleanup

    func cancelAll() {
        stopObservingProfile()
        stopObservingScreenTime()
    }
}

// MARK: - Explicit UserID Overloads
extension ProfileSDKServiceAsync {

    func observeProfile(
        userId: String,
        token: String,
        onEach: @escaping (UserProfile) -> Void,
        onError: @escaping (Error) -> Void
    ) {
        profileCancellable?.cancel()
        profileCancellable = ISDKClient.shared.observeProfile(
            userId: userId,
            token: token,
            onEach: { profile in
                onEach(profile)
            },
            onError: { throwable in
                onError(SDKError.message(throwable.message ?? "Profile observe error"))
            }
        )
    }

    func updateProfile(
        userId: String,
        token: String,
        request: UpdateProfileRequest,
        completion: @escaping (UserProfile?, Error?) -> Void
    ) {
        ISDKClient.shared.updateProfile(userId: userId, token: token, request: request) { result in
            if let success = result as? ResultSuccess<UserProfile> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            }
        }
    }

    func getCompleteProfileData(
        userId: String,
        token: String,
        completion: @escaping (UserProfileData?, Error?) -> Void
    ) {
        ISDKClient.shared.getCompleteProfileData(userId: userId, token: token) { result in
            if let success = result as? ResultSuccess<UserProfileData> {
                completion(success.data, nil)
            } else if let error = result as? ResultError {
                completion(nil, SDKError.message(error.message))
            }
        }
    }

    func observeScreenTime(
        userId: String,
        token: String,
        days: Int32 = 7,
        onEach: @escaping ([ScreenTimeEntry]) -> Void,
        onError: @escaping (Error) -> Void,
        onComplete: @escaping () -> Void
    ) {
        screenTimeCancellable?.cancel()
        screenTimeCancellable = ISDKClient.shared.observeScreenTime(
            userId: userId,
            token: token,
            days: days,
            onEach: { entries in
                onEach(entries as! [ScreenTimeEntry])
            },
            onError: { throwable in
                onError(SDKError.message(throwable.message ?? "Screen time observe error"))
            },
            onComplete: {
                onComplete()
            }
        )
    }
}
