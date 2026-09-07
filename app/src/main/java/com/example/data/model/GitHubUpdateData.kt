package com.example.data.model

data class GitHubCommit(
    val sha: String,
    val shortSha: String,
    val message: String,
    val author: String,
    val date: String,
    val htmlUrl: String
)

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val publishedAt: String,
    val body: String,
    val htmlUrl: String,
    val downloadUrl: String?
)

data class GitHubUpdateState(
    val isLoading: Boolean = false,
    val isAutoRefreshEnabled: Boolean = true,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val repoName: String = "arthurdelneste33-svg/Wireless-Diagnostic-Tool",
    val repoUrl: String = "https://github.com/arthurdelneste33-svg/Wireless-Diagnostic-Tool",
    val defaultBranch: String = "main",
    val currentAppVersion: String = "v2.1.0",
    val latestRemoteVersion: String = "v2.1.0",
    val isUpToDate: Boolean = true,
    val latestCommits: List<GitHubCommit> = emptyList(),
    val latestRelease: GitHubRelease? = null,
    val starsCount: Int = 0,
    val forksCount: Int = 0,
    val openIssuesCount: Int = 0,
    val syncStatusMessage: String = "Connecté au dépôt GitHub",
    val errorMessage: String? = null
)
