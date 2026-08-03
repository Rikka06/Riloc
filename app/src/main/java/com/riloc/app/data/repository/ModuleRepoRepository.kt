package com.riloc.app.data.repository

import com.riloc.app.data.model.RepoModule

interface ModuleRepoRepository {
    suspend fun fetchModules(): Result<List<RepoModule>>
}

