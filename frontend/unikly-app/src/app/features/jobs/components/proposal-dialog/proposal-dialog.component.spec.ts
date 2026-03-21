import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProposalDialogComponent } from './proposal-dialog.component';

describe('ProposalDialogComponent', () => {
  let component: ProposalDialogComponent;
  let fixture: ComponentFixture<ProposalDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProposalDialogComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ProposalDialogComponent);
    component = fixture.componentInstance;
    component.data = { jobTitle: 'Test', jobBudget: 100, jobCurrency: 'USD' };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
