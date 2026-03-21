import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProposalDialogComponent } from './proposal-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

describe('ProposalDialogComponent', () => {
  let component: ProposalDialogComponent;
  let fixture: ComponentFixture<ProposalDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProposalDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { jobTitle: 'Test', jobBudget: 100, jobCurrency: 'USD' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProposalDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
