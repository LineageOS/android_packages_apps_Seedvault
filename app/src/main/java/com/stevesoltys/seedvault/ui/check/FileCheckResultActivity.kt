/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.ui.check

import android.os.Bundle
import com.stevesoltys.seedvault.ui.setupEdgeToEdge
import org.calyxos.backup.storage.api.StorageBackup
import org.calyxos.backup.storage.ui.check.CheckResultActivity
import org.calyxos.backup.storage.ui.restore.FileSelectionManager
import org.koin.android.ext.android.inject

class FileCheckResultActivity : CheckResultActivity() {

    override val storageBackup: StorageBackup by inject()
    override val fileSelectionManager: FileSelectionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        setupEdgeToEdge()
        super.onCreate(savedInstanceState)
    }
}
