//  ProfileSDKService.swift
//  Bridges KMP ISDKClient callbacks to Swift async/await and closures.

import Foundation
import Profile_SDK

struct AppProfile {
    let label: String
    let userId: String
}

enum SDKError: Error {
    case notInitialized
    case message(String)
}

@MainActor
final class ProfileSDKService {

    static let internalKey = "6bebaa8645a54b27b858d74cbf5a1aac9f56b4945d85aa2f7abf0885df540c5b"

    static let profiles: [AppProfile] = [
        AppProfile(label: "P1", userId: "mMCb2xH89eUWqnJPOkV0WfGf2XO2"),
        AppProfile(label: "P2", userId: "TbyvkxSKuBSNSRUdwv9uDfhbCN12"),
        AppProfile(label: "P3", userId: "xPVwfiBkHSX6qBqnufSUssJdDWI2"),
        AppProfile(label: "P4", userId: "wFpRlxosKPXKzjjDqwXpXVicoLc2"),
    ]

    private var profileCancellable: Cancellable?
    private var screenTimeCancellable: Cancellable?

    // MARK: - Initialize + Token

    func initialize(userId: String) {
        ISDKClient.shared.initialize(userId: userId, context: NSObject())
    }

    func generateToken() async throws -> String {
        try await withCheckedThrowingContinuation { continuation in
            ISDKClient.shared.generateToken(internalKey: Self.internalKey) { result in
                if let success = result as? ResultSuccess<NSString> {
                    continuation.resume(returning: success.data! as String)
                } else if let error = result as? ResultError {
                    continuation.resume(throwing: SDKError.message(error.message))
                } else {
                    continuation.resume(throwing: SDKError.message("Unknown token error"))
                }
            }
        }
    }

    // MARK: - Observe Profile

    func observeProfile(
        token: String,
        onEach: @escaping (UserProfile) -> Void,
        onError: @escaping (String) -> Void
    ) {
        profileCancellable?.cancel()
        profileCancellable = ISDKClient.shared.observeProfile(
            token: token,
            onEach: { profile in onEach(profile) },
            onError: { throwable in onError(throwable.message ?? "Profile observe error") }
        )
    }

    func cancelProfileObserver() {
        profileCancellable?.cancel()
        profileCancellable = nil
    }

    // MARK: - Update Profile

    func updateProfile(token: String, current: UserProfile) async throws -> UserProfile {
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

        return try await withCheckedThrowingContinuation { continuation in
            ISDKClient.shared.updateProfile(token: token, request: request) { result in
                if let success = result as? ResultSuccess<UserProfile> {
                    if let profile = success.data {
                        continuation.resume(returning: profile)
                    } else {
                        continuation.resume(throwing: SDKError.message("No profile returned"))
                    }
                } else if let error = result as? ResultError {
                    continuation.resume(throwing: SDKError.message(error.message))
                } else {
                    continuation.resume(throwing: SDKError.message("Unknown update error"))
                }
            }
        }
    }

    // MARK: - Screen Time

    func observeScreenTime(
        token: String,
        days: Int32 = 7,
        onEach: @escaping ([ScreenTimeEntry]) -> Void,
        onError: @escaping (String) -> Void
    ) {
        screenTimeCancellable?.cancel()
        screenTimeCancellable = ISDKClient.shared.observeScreenTime(
            token: token,
            days: days,
            onEach: { entries in onEach(entries as! [ScreenTimeEntry]) },
            onError: { throwable in onError(throwable.message ?? "Screen time observe error") },
            onComplete: {}
        )
    }

    func cancelScreenTimeObserver() {
        screenTimeCancellable?.cancel()
        screenTimeCancellable = nil
    }

    func postScreenTime(token: String) async throws {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let today = formatter.string(from: Date())
        let request = ScreenTimeRequest(date: today, seconds: 60)

        return try await withCheckedThrowingContinuation { continuation in
            ISDKClient.shared.postScreenTime(token: token, days: 7, request: request) { result in
                if result is ResultSuccess<AnyObject> {
                    continuation.resume()
                } else if let error = result as? ResultError {
                    continuation.resume(throwing: SDKError.message(error.message))
                } else {
                    continuation.resume(throwing: SDKError.message("Unknown post screen time error"))
                }
            }
        }
    }

    // MARK: - Profile Views

    func getProfileViews(token: String) async throws -> ProfileViewsData {
        try await withCheckedThrowingContinuation { continuation in
            ISDKClient.shared.getProfileViews(token: token, cursor: nil, limit: nil) { result in
                if let success = result as? ResultSuccess<ProfileViewsData> {
                    if let data = success.data {
                        continuation.resume(returning: data)
                    } else {
                        continuation.resume(throwing: SDKError.message("No profile views data"))
                    }
                } else if let error = result as? ResultError {
                    continuation.resume(throwing: SDKError.message(error.message))
                } else {
                    continuation.resume(throwing: SDKError.message("Unknown profile views error"))
                }
            }
        }
    }

    // MARK: - Nearby Users

    func getNearbyUsers(token: String) async throws -> [NearbyUser] {
        try await withCheckedThrowingContinuation { continuation in
            ISDKClient.shared.getNearbyUsers(token: token, lat: 24.8607, lng: 67.0011) { result in
                if let success = result as? ResultSuccess<NSArray> {
                    let users = (success.data as? [NearbyUser]) ?? []
                    continuation.resume(returning: users)
                } else if let error = result as? ResultError {
                    continuation.resume(throwing: SDKError.message(error.message))
                } else {
                    continuation.resume(throwing: SDKError.message("Unknown nearby users error"))
                }
            }
        }
    }

    // MARK: - Cleanup

    func cancelAll() {
        cancelProfileObserver()
        cancelScreenTimeObserver()
    }
}
