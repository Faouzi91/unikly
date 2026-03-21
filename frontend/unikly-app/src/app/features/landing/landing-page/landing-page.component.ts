import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';

// ─── Static data ─────────────────────────────────────────────────────────────

const TRUST_LOGOS = [
  'Stripe', 'Vercel', 'Notion', 'Linear', 'Figma',
  'Supabase', 'PlanetScale', 'Railway', 'Loom', 'Retool',
];

const STATS = [
  { value: '12k+',  label: 'Vetted experts'   },
  { value: '$2.1B', label: 'Project value'     },
  { value: '98%',   label: 'Match accuracy'    },
  { value: '4.9★',  label: 'Average rating'    },
];

const HOW_IT_WORKS = [
  {
    step: '01', icon: '📋',
    title: 'Post your project',
    desc: 'Describe what you need — budget, timeline, tech stack. Takes under 3 minutes.',
  },
  {
    step: '02', icon: '🤖',
    title: 'AI finds your match',
    desc: 'Our semantic engine ranks thousands of candidates by skills, past performance, and fit score.',
  },
  {
    step: '03', icon: '🚀',
    title: 'Hire and ship',
    desc: 'Review curated profiles, align on scope, and start work the same day. Payments handled.',
  },
];

const CATEGORIES = [
  { icon: '🤖', name: 'AI & Machine Learning', count: '2,400+' },
  { icon: '⚛️', name: 'Frontend Engineering',  count: '3,100+' },
  { icon: '⚙️', name: 'Backend & APIs',         count: '2,800+' },
  { icon: '☁️', name: 'Cloud & DevOps',          count: '1,700+' },
  { icon: '🔗', name: 'Web3 & Blockchain',       count: '890+'  },
  { icon: '🎨', name: 'UX Architecture',         count: '1,200+' },
  { icon: '🔒', name: 'Security & Auditing',     count: '640+'  },
  { icon: '📱', name: 'Mobile (iOS & Android)',  count: '1,500+' },
];

const FEATURED_JOBS = [
  {
    type: 'Fixed Budget', budget: '$5k – $12k',
    title: 'Build a Next-Gen AI Analytics Dashboard',
    desc: 'Senior frontend engineer to architect a high-performance analytics platform with Tailwind 4 + React and real-time WebSocket data streams.',
    tags: ['React', 'Tailwind', 'WebSocket', 'TypeScript'],
    company: 'TechFlow Inc.',
    posted: '2h ago',
  },
  {
    type: 'Hourly', budget: '$120 / hr',
    title: 'Rust Backend for High-Frequency Trading Engine',
    desc: 'Design and implement a low-latency order-matching engine with sub-millisecond p99 response times in a regulated environment.',
    tags: ['Rust', 'Systems', 'Finance', 'PostgreSQL'],
    company: 'QuantEdge',
    posted: '5h ago',
  },
  {
    type: 'Fixed Budget', budget: '$8k – $20k',
    title: 'Smart Contract Audit & DeFi Protocol Security Review',
    desc: 'Full security audit of an EVM-based DeFi lending protocol. Deliverables: vulnerability report, PoCs, and remediation guide.',
    tags: ['Solidity', 'Web3', 'Security', 'EVM'],
    company: 'ChainVault Labs',
    posted: '1d ago',
  },
];

const TESTIMONIALS = [
  {
    initials: 'SR', name: 'Sofia R.', role: 'CTO — FinScale',
    color: 'bg-blue-500/20 text-blue-400',
    quote: 'Unikly matched us with a senior Rust engineer in 4 hours. The profile accuracy was uncanny — saved us weeks of sourcing and two rounds of bad interviews.',
  },
  {
    initials: 'JK', name: 'James K.', role: 'Founder — NovaMesh',
    color: 'bg-purple-500/20 text-purple-400',
    quote: 'As a freelancer, Unikly sends me only relevant projects. I landed 3 long-term contracts in my first month without a single cold pitch or résumé submission.',
  },
  {
    initials: 'ML', name: 'Mia L.', role: 'VP Eng — CloudPeak',
    color: 'bg-accent/20 text-accent',
    quote: "The escrow-based payment system is the most transparent I've used. Zero payment disputes across 12 projects spanning 4 different time zones.",
  },
];

const FOOTER_LINKS = [
  {
    heading: 'Platform',
    links: ['Hire Talent', 'Find Projects', 'AI Matching', 'Escrow Payments'],
  },
  {
    heading: 'Company',
    links: ['About', 'Blog', 'Careers', 'Contact'],
  },
  {
    heading: 'Legal',
    links: ['Privacy Policy', 'Terms of Service', 'Security', 'Cookie Policy'],
  },
];

const SOCIALS = [
  { icon: '𝕏', label: 'Twitter / X' },
  { icon: 'in', label: 'LinkedIn'    },
  { icon: '⌨', label: 'GitHub'      },
];

// ─── Component ────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent {
  readonly theme = inject(ThemeService);

  readonly trustLogos   = TRUST_LOGOS;
  readonly stats        = STATS;
  readonly howItWorks   = HOW_IT_WORKS;
  readonly categories   = CATEGORIES;
  readonly featuredJobs = FEATURED_JOBS;
  readonly testimonials = TESTIMONIALS;
  readonly footerLinks  = FOOTER_LINKS;
  readonly socials      = SOCIALS;

  readonly aiMatches = [
    { initials: 'AS', name: 'Alex S.',  role: 'Fullstack Engineer',  score: 98, color: 'bg-blue-500/20 text-blue-400'   },
    { initials: 'MK', name: 'Maria K.', role: 'AI Specialist',        score: 94, color: 'bg-purple-500/20 text-purple-400' },
    { initials: 'RL', name: 'Ryan L.',  role: 'Cloud Architect',      score: 91, color: 'bg-amber-500/20 text-amber-400'  },
  ];
}
