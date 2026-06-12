/*
 * Copyright (C) 2026 Blackilykat and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat.pmp.client.qt;

import io.qt.NonNull;
import io.qt.core.QAbstractListModel;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import java.util.ArrayList;
import java.util.List;


abstract class ReplaceableListModel<T> extends QAbstractListModel {
	protected final List<T> items = new ArrayList<>();

	public ReplaceableListModel(QObject parent) {
		super(parent);
	}

	@Override
	public int rowCount(@NonNull QModelIndex arg0) {
		return items.size();
	}

	public void add(T item, int index) {
		beginInsertRows(new QModelIndex(), index, index);
		items.add(index, item);
		endInsertRows();
	}

	public void addMany(List<T> newItems, int dest, int src, int count) {
		beginInsertRows(new QModelIndex(), dest, dest + count);
		for(int i = 0; i < count; i++) {
			items.add(dest + i, newItems.get(src + i));
		}
		endInsertRows();
	}

	public void remove(int index, int count) {
		beginRemoveRows(new QModelIndex(), index, index + count - 1);
		for(int i = 0; i < count; i++) {
			items.remove(index);
		}
		endRemoveRows();
	}

	public void replace(List<T> newItems) {
		int i = 0;
		int j = 0;
		for(; i < items.size(); i++, j++) {
			if(j >= newItems.size()) {
				remove(i, items.size() - i);
				break;
			}

			if(items.get(i) == newItems.get(j)) {
				continue;
			}

			T newItem = newItems.get(j);
			if(items.contains(newItem)) {
				while(i < items.size() && items.get(i) != newItem) {
					remove(i, 1);
				}
				continue;
			}

			add(newItem, i);
		}

		if(j < newItems.size()) {
			addMany(newItems, items.size(), j, newItems.size() - j);
		}
	}

	public void clear() {
		if(items.isEmpty()) return;
		beginRemoveRows(new QModelIndex(), 0, items.size() - 1);
		items.clear();
		endRemoveRows();
	}
}

