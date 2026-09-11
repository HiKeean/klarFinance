import { Directive, Input, TemplateRef } from '@angular/core';

export interface TableCellContext<T = unknown> {
  $implicit: T;
  row: T;
}

/**
 * Marks an <ng-template appTableCell="columnKey" let-row> as the custom cell
 * renderer for that column in a parent <app-table>. Columns without a
 * matching template fall back to plain-text rendering of column.key.
 */
@Directive({
  selector: '[appTableCell]',
  standalone: true,
})
export class TableCellDirective<T = unknown> {
  @Input('appTableCell') columnKey = '';

  constructor(public readonly templateRef: TemplateRef<TableCellContext<T>>) {}
}
