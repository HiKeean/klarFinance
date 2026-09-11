import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, ContentChildren, EventEmitter, Input, Output, QueryList } from '@angular/core';

import { TableCellDirective } from './table-cell.directive';
import { TableColumn } from './table-column';

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './table.html',
  styleUrl: './table.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableComponent<T = Record<string, unknown>> {
  @Input({ required: true }) columns: TableColumn<T>[] = [];
  @Input({ required: true }) rows: T[] = [];

  @Input() loading = false;
  @Input() loadingMessage = 'Loading...';
  @Input() errorMessage = '';
  @Input() emptyMessage = 'No data found.';

  /** Defaults to row index; override for a stable identity (e.g. row.id). */
  @Input() trackBy: (row: T, index: number) => unknown = (_row, index) => index;

  /** Optional per-row class (e.g. to dim a soft-deleted row). */
  @Input() rowClass: (row: T, index: number) => string = () => '';

  /** Set to true to show the built-in search box above the table. */
  @Input() search = false;
  @Input() searchValue = '';
  @Input() searchPlaceholder = 'Search...';
  @Output() searchValueChange = new EventEmitter<string>();

  /** Emits the row when a data row is clicked. Listen to this to make rows navigable. */
  @Output() rowClick = new EventEmitter<T>();

  @ContentChildren(TableCellDirective) cellTemplates?: QueryList<TableCellDirective<T>>;

  templateFor(columnKey: string): TableCellDirective<T> | undefined {
    return this.cellTemplates?.find((template) => template.columnKey === columnKey);
  }

  onSearchInput(event: Event): void {
    this.searchValueChange.emit((event.target as HTMLInputElement).value);
  }

  cellValue(row: T, key: string): unknown {
    return key.split('.').reduce<unknown>((value, part) => {
      if (value && typeof value === 'object') {
        return (value as Record<string, unknown>)[part];
      }
      return undefined;
    }, row);
  }
}
