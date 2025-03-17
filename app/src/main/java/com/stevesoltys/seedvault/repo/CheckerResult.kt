/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.stevesoltys.seedvault.repo

import com.stevesoltys.seedvault.proto.Snapshot

sealed class CheckerResult {
    data class Success(
        val snapshots: List<Snapshot>,
        val percent: Int,
        val size: Long,
    ) : CheckerResult()

    data class Error(
        /**
         * This number is greater than the size of [snapshots],
         * if we could not read/decrypt one or more snapshots.
         */
        val existingSnapshots: Int,
        val snapshots: List<Snapshot>,
        /**
         * The list of chunkIDs that had errors.
         */
        val errorChunkIdBlobPairs: Set<ChunkIdBlobPair>,
    ) : CheckerResult() {
        val goodSnapshots: List<Snapshot>
        val badSnapshots: List<Snapshot>

        init {
            val good = mutableListOf<Snapshot>()
            val bad = mutableListOf<Snapshot>()
            val errorChunkIds = errorChunkIdBlobPairs.map { it.chunkId }.toSet()
            snapshots.forEach { snapshot ->
                val badChunkIds = snapshot.blobsMap.keys.intersect(errorChunkIds)
                if (badChunkIds.isEmpty()) {
                    // snapshot doesn't contain chunks with erroneous blobs
                    good.add(snapshot)
                } else {
                    // snapshot may contain chunks with erroneous blobs, check deeper
                    val isBad = badChunkIds.any { chunkId ->
                        val blob = snapshot.blobsMap[chunkId] ?: error("No blob for chunkId")
                        // is this chunkId/blob pair in errorChunkIdBlobPairs?
                        errorChunkIdBlobPairs.any { pair ->
                            pair.chunkId == chunkId && pair.blob == blob
                        }
                    }
                    if (isBad) bad.add(snapshot) else good.add(snapshot)
                }
            }
            goodSnapshots = good
            badSnapshots = bad
        }
    }

    data class GeneralError(val e: Exception) : CheckerResult()
}
