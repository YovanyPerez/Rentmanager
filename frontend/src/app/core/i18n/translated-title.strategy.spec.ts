import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router, TitleStrategy, provideRouter } from '@angular/router';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { TranslatedTitleStrategy } from './translated-title.strategy';

@Component({ selector: 'app-title-blank', template: '' })
class Blank {}

describe('TranslatedTitleStrategy', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TranslocoTestingModule.forRoot({
          preloadLangs: true,
          langs: {
            es: { app: { title: 'RentManager' }, home: { title: 'Inicio' } },
            en: { app: { title: 'RentManager' }, home: { title: 'Home' } },
          },
          translocoConfig: {
            availableLangs: ['es', 'en'],
            defaultLang: 'es',
            fallbackLang: 'en',
          },
        }),
      ],
      providers: [
        provideRouter([{ path: 'home', component: Blank, title: 'home.title' }]),
        { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
      ],
    }).compileComponents();
  });

  it('sets the localised route title', async () => {
    await TestBed.inject(Router).navigateByUrl('/home');

    expect(TestBed.inject(Title).getTitle()).toBe('Inicio');
  });

  it('updates the title when the language changes', async () => {
    await TestBed.inject(Router).navigateByUrl('/home');

    TestBed.inject(TranslocoService).setActiveLang('en');

    expect(TestBed.inject(Title).getTitle()).toBe('Home');
  });
});
