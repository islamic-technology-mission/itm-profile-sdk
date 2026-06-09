//  ProfileSDKDemoViewModel.swift

import Foundation
import Combine
import Profile_SDK

// MARK: - SDKError display helper

private extension SDKError {
    /// Returns the actual error message, not Swift's generic NSError description.
    var displayMessage: String {
        switch self {
        case .message(let msg): return msg.isEmpty ? "An unknown SDK error occurred." : msg
        case .notInitialized:   return "SDK not initialized. Call initialize() first."
        }
    }
}

// MARK: - ViewModel

@MainActor
final class ProfileSDKDemoViewModel: ObservableObject {

    @Published var selectedProfileIndex: Int = 0
    @Published var profileState: ViewState<UserProfile> = .idle
    @Published var screenTimeState: ViewState<[ScreenTimeEntry]> = .idle
    @Published var profileViewsState: ViewState<ProfileViewsData> = .idle
    @Published var nearbyUsersState: ViewState<[NearbyUser]> = .idle
    @Published var updateProfileState: ViewState<UserProfile> = .idle
    @Published var postScreenTimeState: ViewState<String> = .idle
    @Published var sdkStatus: String = "Not initialized"

    private let service = ProfileSDKService()
    private var currentToken: String?
    private var currentProfile: UserProfile?
    private var loadTask: Task<Void, Never>?

    // Incremented each time loadSelectedProfile() runs.
    // Callbacks that arrive carrying a stale generation are discarded.
    private var loadGeneration: Int = 0

    var profiles: [AppProfile] { ProfileSDKService.profiles }

    // MARK: - Profile Selection

    func selectProfile(_ index: Int) {
        guard index != selectedProfileIndex || currentToken == nil else { return }
        selectedProfileIndex = index
        loadSelectedProfile()
    }

    func loadSelectedProfile() {
        loadTask?.cancel()
        service.cancelAll()

        currentToken = nil
        currentProfile = nil
        profileState = .loading
        screenTimeState = .loading
        profileViewsState = .idle
        nearbyUsersState = .idle
        updateProfileState = .idle
        postScreenTimeState = .idle
        sdkStatus = "Initializing…"

        loadGeneration &+= 1                    // overflow-safe increment
        let generation = loadGeneration
        let profile = profiles[selectedProfileIndex]

        loadTask = Task {
            service.initialize(userId: profile.userId)
            guard !Task.isCancelled else { return }
            sdkStatus = "Generating token…"

            do {
                let token = try await service.generateToken()
                guard !Task.isCancelled, generation == loadGeneration else { return }
                currentToken = token
                sdkStatus = "Token ready. Loading profile…"

                service.observeProfile(token: token) { [weak self] userProfile in
                    guard let self, generation == self.loadGeneration else { return }
                    self.currentProfile = userProfile
                    self.profileState = .success(userProfile)
                    self.sdkStatus = "Profile loaded"
                } onError: { [weak self] message in
                    guard let self, generation == self.loadGeneration else { return }
                    self.profileState = .failure(message)
                    self.sdkStatus = "Profile error: \(message)"
                }

                service.observeScreenTime(token: token) { [weak self] entries in
                    guard let self, generation == self.loadGeneration else { return }
                    self.screenTimeState = entries.isEmpty ? .empty : .success(entries)
                } onError: { [weak self] message in
                    guard let self, generation == self.loadGeneration else { return }
                    self.screenTimeState = .failure(message)
                }

            } catch let sdkErr as SDKError {
                // Extract the real message from SDKError instead of using
                // localizedDescription, which hides it behind "SDKError error N".
                guard !Task.isCancelled, generation == loadGeneration else { return }
                let msg = sdkErr.displayMessage
                profileState = .failure(msg)
                screenTimeState = .failure(msg)
                sdkStatus = "Error: \(msg)"
            } catch {
                guard !Task.isCancelled, generation == loadGeneration else { return }
                profileState = .failure(error.localizedDescription)
                screenTimeState = .failure(error.localizedDescription)
                sdkStatus = "Error: \(error.localizedDescription)"
            }
        }
    }

    // MARK: - Update Profile

    func updateProfile() {
        guard let token = currentToken, let profile = currentProfile else { return }
        updateProfileState = .loading
        Task {
            do {
                let updated = try await service.updateProfile(token: token, current: profile)
                currentProfile = updated
                profileState = .success(updated)
                updateProfileState = .success(updated)
            } catch let sdkErr as SDKError {
                updateProfileState = .failure(sdkErr.displayMessage)
            } catch {
                updateProfileState = .failure(error.localizedDescription)
            }
        }
    }

    // MARK: - Screen Time

    func addScreenTime() {
        guard let token = currentToken else { return }
        postScreenTimeState = .loading
        Task {
            do {
                try await service.postScreenTime(token: token)
                postScreenTimeState = .success("Screen time posted (+60s)")
            } catch let sdkErr as SDKError {
                postScreenTimeState = .failure(sdkErr.displayMessage)
            } catch {
                postScreenTimeState = .failure(error.localizedDescription)
            }
        }
    }

    // MARK: - Profile Views

    func loadProfileViews() {
        guard let token = currentToken else { return }
        profileViewsState = .loading
        Task {
            do {
                let data = try await service.getProfileViews(token: token)
                let items = data.items
                if let items, !items.isEmpty {
                    profileViewsState = .success(data)
                } else if let total = data.total, total.intValue > 0 {
                    profileViewsState = .success(data)
                } else {
                    profileViewsState = .empty
                }
            } catch let sdkErr as SDKError {
                profileViewsState = .failure(sdkErr.displayMessage)
            } catch {
                profileViewsState = .failure(error.localizedDescription)
            }
        }
    }

    // MARK: - Nearby Users

    func loadNearbyUsers() {
        guard let token = currentToken else { return }
        nearbyUsersState = .loading
        Task {
            do {
                let users = try await service.getNearbyUsers(token: token)
                nearbyUsersState = users.isEmpty ? .empty : .success(users)
            } catch let sdkErr as SDKError {
                nearbyUsersState = .failure(sdkErr.displayMessage)
            } catch {
                nearbyUsersState = .failure(error.localizedDescription)
            }
        }
    }
}
