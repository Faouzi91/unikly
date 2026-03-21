import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent {
  readonly theme = inject(ThemeService);

  readonly logos = ['Shopify', 'Notion', 'Airbyte', 'Linear', 'Ramp', 'Figma', 'Vercel', 'Datadog'];

  readonly stats = [
    { value: '12K+', label: 'Verified freelancers' },
    { value: '$2.1B', label: 'Annual project volume' },
    { value: '97%', label: 'Hiring success rate' },
    { value: '4.9/5', label: 'Average client rating' },
  ];

  readonly steps = [
    {
      title: 'Post your need',
      description: 'Share scope, budget, timeline, and required skills in a structured brief.',
    },
    {
      title: 'Get ranked matches',
      description: 'AI ranking highlights the best-fit freelancers based on quality and delivery history.',
    },
    {
      title: 'Hire and deliver',
      description: 'Collaborate through milestones, messages, and escrow-backed payments from one dashboard.',
    },
  ];

  readonly categories = [
    { name: 'Web Development', count: '3,100+' },
    { name: 'Mobile Engineering', count: '1,420+' },
    { name: 'UI and Product Design', count: '1,900+' },
    { name: 'Cloud and DevOps', count: '1,120+' },
    { name: 'Data and AI', count: '2,300+' },
    { name: 'Cybersecurity', count: '780+' },
  ];

  readonly featuredJobs = [
    {
      type: 'Fixed Price',
      budget: '$6,000 - $14,000',
      title: 'Build an AI-powered analytics portal',
      summary: 'Need a senior Angular engineer to deliver a responsive analytics interface with charting and export flows.',
      tags: ['Angular', 'Tailwind', 'TypeScript', 'Charts'],
    },
    {
      type: 'Hourly',
      budget: '$90/hr',
      title: 'Scale payment orchestration microservices',
      summary: 'Backend specialist needed for resilient Java services, idempotent workflows, and observability improvements.',
      tags: ['Java', 'Spring', 'Kafka', 'PostgreSQL'],
    },
    {
      type: 'Fixed Price',
      budget: '$8,500 - $18,000',
      title: 'Design a modern enterprise SaaS UI',
      summary: 'Product designer to craft full UX flows for a B2B platform with heavy data and admin tooling.',
      tags: ['UX', 'Design Systems', 'Figma', 'B2B'],
    },
  ];

  readonly testimonials = [
    {
      name: 'Sofia Rahman',
      role: 'CTO, FinScale',
      quote: 'We replaced a six-week hiring loop with a two-day shortlist and shipped our release on schedule.',
    },
    {
      name: 'Marc K.',
      role: 'Freelance Architect',
      quote: 'Project recommendations are relevant and high quality. I spend less time bidding and more time building.',
    },
    {
      name: 'Aline M.',
      role: 'VP Product, CloudPeak',
      quote: 'Milestone-based escrow gave our team confidence during cross-border contracts and reduced payment friction.',
    },
  ];
}
