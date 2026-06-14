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

import java.util.HashMap;
import java.util.Map;

import dev.blackilykat.pmp.client.ClientStorage;
import dev.blackilykat.pmp.client.Filter;
import dev.blackilykat.pmp.client.FilterOption;
import dev.blackilykat.pmp.client.Library;
import io.qt.NonNull;
import io.qt.QtPrimitiveType;
import io.qt.core.QByteArray;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QVariant;
import io.qt.gui.QGuiApplication;
import io.qt.qml.QQmlContext;

class Filters {
	public static void initialize(QQmlContext context) {
		FilterListModel filterListModel = new FilterListModel(QGuiApplication.instance());
		context.setContextProperty("filterListModel", new QVariant(filterListModel));
	}

	public static class FilterListModel extends ReplaceableListModel<Filter> {
		public static final int ROLE_KEY = 0x0101;
		public static final int ROLE_ID = 0x0102;
		public static final int ROLE_OPTIONS = 0x0103;

		private Map<Filter, FilterOptionListModel> options = new HashMap<>();

		public FilterListModel(QObject parent) {
			super(parent);

			replace(ClientStorage.MAIN.filters.get());
			Library.EVENT_FILTERS_UPDATED.register(this::replace);
		}

		@Override
		public @NonNull Map<@QtPrimitiveType @NonNull Integer, @NonNull QByteArray> roleNames() {
			return Map.of(
				ROLE_KEY, new QByteArray("key"),
				ROLE_ID, new QByteArray("id"),
				ROLE_OPTIONS, new QByteArray("options")
			);
		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			Filter row = items.get(arg0.row());
			return switch(arg1) {
				case ROLE_KEY -> row.key;
				case ROLE_ID -> row.id;
				case ROLE_OPTIONS -> {
					var ret = options.get(row);
					if(ret == null) {
						ret = new FilterOptionListModel(parent(), row);
						options.put(row, ret);
					}
					yield ret;
				}
				default -> new QVariant();
			};
		}
	}

	public static class FilterOptionListModel extends ReplaceableListModel<FilterOption> {
		public static final int ROLE_NAME = 0x0101;
		public static final int ROLE_STATE = 0x0102;

		public FilterOptionListModel(QObject parent, Filter filter) {
			super(parent);

			replace(filter.getOptions());

			for(var item : items) {
				item.eventChangedState.register(_ -> {
					int index = items.indexOf(item);
					dataChanged.emit(index(index, 0), index(index, 0));
				});
			}

			filter.eventOptionAdded.register(event -> {
				var item = event.option();
				add(item, event.index());

				item.eventChangedState.register(_ -> {
					int index = items.indexOf(item);
					dataChanged.emit(index(index, 0), index(index, 0));
				});
			});

			filter.eventOptionRemoved.register(event -> {
				remove(items.indexOf(event.option()), 1);
				// listener will get garbage collected, no need to clean up
			});
		}

		@Override
		public @NonNull Map<@QtPrimitiveType @NonNull Integer, @NonNull QByteArray> roleNames() {
			return Map.of(
				ROLE_NAME, new QByteArray("name"),
				ROLE_STATE, new QByteArray("optionState")
			);
		}

		@Override
		public Object data(@NonNull QModelIndex arg0, int arg1) {
			var row = items.get(arg0.row());

			return switch(arg1) {
				case ROLE_NAME -> row.value;
				case ROLE_STATE -> row.getState().toString();
				default -> new QVariant();
			};
		}
	}
}
