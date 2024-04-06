/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.model.data

import android.content.Context
import com.android.launcher3.LauncherSettings
import com.android.launcher3.icons.IconCache
import com.android.launcher3.logger.LauncherAtom
import com.android.launcher3.views.ActivityContext

/** A type of app collection that launches multiple apps into split screen. */
class AppPairInfo() : CollectionInfo() {
    private var contents: ArrayList<WorkspaceItemInfo> = ArrayList()

    init {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_APP_PAIR
    }

    /** Convenience constructor, calls primary constructor and init block */
    constructor(app1: WorkspaceItemInfo, app2: WorkspaceItemInfo) : this() {
        add(app1)
        add(app2)
    }

    /** Creates a new AppPairInfo that is a copy of the provided one. */
    constructor(appPairInfo: AppPairInfo) : this() {
        contents = appPairInfo.contents.clone() as ArrayList<WorkspaceItemInfo>
        copyFrom(appPairInfo)
    }

    /** Adds an element to the contents ArrayList. */
    override fun add(item: ItemInfo) {
        if (item !is WorkspaceItemInfo) {
            throw RuntimeException("tried to add an illegal type into an app pair")
        }

        contents.add(item)
    }

    /** Returns the app pair's member apps as an ArrayList of [ItemInfo]. */
    override fun getContents(): ArrayList<ItemInfo> =
        ArrayList(contents.stream().map { it as ItemInfo }.toList())

    /** Returns the app pair's member apps as an ArrayList of [WorkspaceItemInfo]. */
    override fun getAppContents(): ArrayList<WorkspaceItemInfo> = contents

    /** Returns the first app in the pair. */
    fun getFirstApp() = contents[0]

    /** Returns the second app in the pair. */
    fun getSecondApp() = contents[1]

    /** Returns if either of the app pair members is currently disabled. */
    override fun isDisabled() = anyMatch { it.isDisabled }

    /** Checks if the app pair is launchable at the current screen size. */
    fun isLaunchable(context: Context) =
        (ActivityContext.lookupContext(context) as ActivityContext).getDeviceProfile().isTablet ||
            getAppContents().stream().noneMatch {
                it.hasStatusFlag(WorkspaceItemInfo.FLAG_NON_RESIZEABLE)
            }

    /** Fetches high-res icons for member apps if needed. */
    fun fetchHiResIconsIfNeeded(iconCache: IconCache) {
        getAppContents().stream().filter(ItemInfoWithIcon::usingLowResIcon).forEach { member ->
            iconCache.getTitleAndIcon(member, false)
        }
    }

    /** Generates an ItemInfo for logging. */
    override fun buildProto(cInfo: CollectionInfo?): LauncherAtom.ItemInfo {
        val appPairIcon = LauncherAtom.FolderIcon.newBuilder().setCardinality(contents.size)
        appPairIcon.setLabelInfo(title.toString())
        return getDefaultItemInfoBuilder()
            .setFolderIcon(appPairIcon)
            .setRank(rank)
            .setContainerInfo(getContainerInfo())
            .build()
    }
}