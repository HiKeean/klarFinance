export interface TableColumn<T = Record<string, unknown>> {
  /**
   * Dot-path into the row object (e.g. 'name' or 'branch.name') used for the
   * default text cell, and the key custom cell templates match against via
   * appTableCell.
   */
  key: string;
  header: string;
  headerClass?: string;
  cellClass?: string;
  /** CSS width applied to the header cell (e.g. '20%', '120px'). */
  width?: string;
}
