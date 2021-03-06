/*
 * Copyright (C) 2016 - 2018 Brian Wernick
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

package com.devbrackets.android.recyclerext.adapter.header;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class HeaderCore {
    @NonNull
    protected HeaderApi headerApi;
    @NonNull
    protected HeaderDataGenerator.HeaderData headerData = new HeaderDataGenerator.HeaderData();

    @NonNull
    protected HeaderAdapterDataObserver observer;
    protected boolean registeredObserver = false;

    protected boolean autoUpdateHeaders = true;

    public HeaderCore(@NonNull HeaderApi api) {
        this.headerApi = api;
        observer = new HeaderAdapterDataObserver(this, api);
    }

    public void showHeaderAsChild(boolean enabled) {
        headerData.showHeaderAsChild = enabled;

        if (autoUpdateHeaders) {
            observer.onChanged();
        }
    }

    @NonNull
    public HeaderDataGenerator.HeaderData getHeaderData() {
        return headerData;
    }

    public void setHeaderData(@NonNull HeaderDataGenerator.HeaderData headerData) {
        this.headerData = headerData;
    }

    public boolean getAutoUpdateHeaders() {
        return autoUpdateHeaders;
    }

    public void setAutoUpdateHeaders(@NonNull RecyclerView.Adapter adapter, boolean autoUpdateHeaders) {
        if (autoUpdateHeaders == this.autoUpdateHeaders) {
            return;
        }

        this.autoUpdateHeaders = autoUpdateHeaders;
        if (autoUpdateHeaders) {
            registerObserver(adapter);
        } else {
            unregisterObserver(adapter);
        }
    }

    /**
     * Returns the total number of views that are associated with the specified
     * header id.  If the headerId doesn't exist then 0 will be returned.
     *
     * @param headerId The headerId to find the number of children for
     * @return The number of children views associated with the given <code>headerId</code>
     */
    public int getChildCount(long headerId) {
        if (headerId == RecyclerView.NO_ID) {
            return 0;
        }

        HeaderItem headerItem = headerData.headerItems.get(headerId);
        return headerItem != null ? headerItem.getChildCount() : 0;
    }

    /**
     * Returns the total number of headers in the list
     *
     * @return The number of headers for the list
     */
    public int getHeaderCount() {
        return headerData.headerItems.size();
    }

    /**
     * Returns a list containing the adapter positions for the
     * headers
     *
     * @return A list of the adapter positions for headers
     */
    @NonNull
    public List<Integer> getHeaderPositions() {
        List<Integer> positions = new ArrayList<>();

        for (int i = 0; i < headerData.headerItems.size(); i++) {
            positions.add(headerData.headerItems.get(headerData.headerItems.keyAt(i)).getAdapterPosition());
        }

        return positions;
    }

    /**
     * Returns the total number of items in the adapter including
     * headers and children.
     *
     * @return The number of items to display in the adapter
     */
    public int getItemCount() {
        return headerData.adapterPositionItemMap.size();
    }

    /**
     * Determines if the item at the <code>adapterPosition</code> is a Header
     * view.
     *
     * @param adapterPosition The raw adapterPosition in the RecyclerView
     * @return True if the item at <code>adapterPosition</code> is a Header
     */
    public boolean isHeader(int adapterPosition) {
        HeaderDataGenerator.AdapterItem item = headerData.adapterPositionItemMap.get(adapterPosition);
        return item != null && item.headerItem != null;
    }

    /**
     * @param parent The parent ViewGroup for the ViewHolder
     * @param viewType The type for the ViewHolder
     * @return The correct ViewHolder for the specified viewType
     */
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if ((viewType & HeaderApi.HEADER_VIEW_TYPE_MASK) != 0) {
            return headerApi.onCreateHeaderViewHolder(parent, viewType);
        }

        return headerApi.onCreateChildViewHolder(parent, viewType);
    }

    /**
     * Retrieves the view type for the specified position.
     *
     * @param adapterPosition The position to determine the view type for
     * @return The type of ViewHolder for the <code>position</code>
     */
    public int getItemViewType(int adapterPosition) {
        return headerData.adapterPositionItemMap.get(adapterPosition).itemViewType;
    }

    /**
     * When the RecyclerView is attached a data observer is registered
     * in order to determine when to re-calculate the headers
     *
     * @param adapter The RecyclerView.Adapter that the observer will be registered for
     */
    public void registerObserver(@NonNull RecyclerView.Adapter adapter) {
        if (autoUpdateHeaders) {
            adapter.registerAdapterDataObserver(observer);
            observer.onChanged();
            registeredObserver = true;
        }
    }

    /**
     * When the RecyclerView is detached the registered data observer
     * will be unregistered.  See {@link #registerObserver(RecyclerView.Adapter)}
     * for more information
     *
     * @param adapter The RecyclerView.Adapter that the observer needs to be unregistered for
     */
    public void unregisterObserver(@NonNull RecyclerView.Adapter adapter) {
        if (registeredObserver) {
            adapter.unregisterAdapterDataObserver(observer);
            registeredObserver = false;
        }

        if (autoUpdateHeaders) {
            headerData.headerItems.clear();
        }
    }

    /**
     * Determines the child position given the position in the RecyclerView
     *
     * @param adapterPosition The position in the RecyclerView (includes Headers and Children)
     * @return The child index
     */
    public int getChildPosition(int adapterPosition) {
        HeaderDataGenerator.AdapterItem item = headerData.adapterPositionItemMap.get(adapterPosition);
        return item != null ? item.childPosition : adapterPosition;
    }

    /**
     * Determines the adapter position given the child position in
     * the RecyclerView
     *
     * @param childPosition The child position
     * @return The adapter position
     */
    public int getAdapterPositionForChild(int childPosition) {
        if (headerData.showHeaderAsChild) {
            return childPosition;
        }

        for (int i = 0; i < headerData.adapterPositionItemMap.size(); i++) {
            int adapterPosition = headerData.adapterPositionItemMap.keyAt(i);
            if (headerData.adapterPositionItemMap.get(adapterPosition).childPosition == childPosition) {
                return adapterPosition;
            }
        }

        return childPosition;
    }

    /**
     * Determines the adapter position for the header associated with
     * the <code>headerId</code>
     *
     * @param headerId The id to find the header for
     * @return The associated headers position or {@link RecyclerView#NO_POSITION}
     */
    public int getHeaderPosition(long headerId) {
        if (headerId == RecyclerView.NO_ID) {
            return RecyclerView.NO_POSITION;
        }

        HeaderItem headerItem = headerData.headerItems.get(headerId);
        return headerItem != null ? headerItem.getAdapterPosition() : RecyclerView.NO_POSITION;
    }

    @Nullable
    public HeaderItem getHeaderForAdapterPosition(@IntRange(from = 0) int adapterPosition) {
        HeaderDataGenerator.AdapterItem item = headerData.adapterPositionItemMap.get(adapterPosition);
        return item != null ? item.headerItem : null;
    }
}