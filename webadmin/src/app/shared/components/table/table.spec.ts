import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TableComponent } from './table';
import { TableColumn } from './table-column';

interface Row {
  name: string;
  branch: { name: string } | null;
}

describe('TableComponent', () => {
  let fixture: ComponentFixture<TableComponent<Row>>;
  let component: TableComponent<Row>;

  const columns: TableColumn<Row>[] = [
    { key: 'name', header: 'Name' },
    { key: 'branch.name', header: 'Branch' }
  ];
  const rows: Row[] = [{ name: 'A', branch: { name: 'HO' } }, { name: 'B', branch: null }];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TableComponent] }).compileComponents();

    fixture = TestBed.createComponent<TableComponent<Row>>(TableComponent);
    component = fixture.componentInstance;
    component.columns = columns;
    component.rows = rows;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('cellValue', () => {
    it('[positive] resolves a plain top-level key', () => {
      expect(component.cellValue(rows[0], 'name')).toBe('A');
    });

    it('[positive] resolves a dot-path into a nested object', () => {
      expect(component.cellValue(rows[0], 'branch.name')).toBe('HO');
    });

    it('[negative] returns undefined when a nested key does not exist', () => {
      expect(component.cellValue(rows[1], 'branch.name')).toBeUndefined();
    });

    it('[negative] returns undefined for an unknown top-level key', () => {
      expect(component.cellValue(rows[0], 'missing')).toBeUndefined();
    });
  });

  describe('templateFor', () => {
    it('[negative] returns undefined when no cell templates are registered', () => {
      expect(component.templateFor('name')).toBeUndefined();
    });
  });

  describe('trackBy default', () => {
    it('[positive] defaults to row index', () => {
      expect(component.trackBy(rows[0], 0)).toBe(0);
      expect(component.trackBy(rows[1], 1)).toBe(1);
    });
  });

  describe('rowClass default', () => {
    it('[positive] defaults to an empty string', () => {
      expect(component.rowClass(rows[0], 0)).toBe('');
    });
  });

  describe('onSearchInput', () => {
    it('[positive] emits the input value via searchValueChange', () => {
      const spy = jasmine.createSpy('searchValueChange');
      component.searchValueChange.subscribe(spy);
      const input = document.createElement('input');
      input.value = 'budi';

      component.onSearchInput({ target: input } as unknown as Event);

      expect(spy).toHaveBeenCalledWith('budi');
    });
  });
});
