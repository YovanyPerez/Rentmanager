import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Meta } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { RobotsService } from './robots.service';

@Component({ selector: 'app-blank', template: '' })
class Blank {}

describe('RobotsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'private', component: Blank },
          { path: 'public', component: Blank, data: { public: true } },
        ]),
      ],
    });
  });

  it('adds noindex to private routes by default', async () => {
    TestBed.inject(RobotsService);

    await TestBed.inject(Router).navigateByUrl('/private');

    const tag = TestBed.inject(Meta).getTag('name="robots"');
    expect(tag?.content).toBe('noindex, nofollow');
  });

  it('removes noindex on public routes', async () => {
    TestBed.inject(RobotsService);

    await TestBed.inject(Router).navigateByUrl('/private');
    await TestBed.inject(Router).navigateByUrl('/public');

    expect(TestBed.inject(Meta).getTag('name="robots"')).toBeNull();
  });
});
