import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CatalogService } from '../catalog/catalog.service';
import { CourseOutline, CourseOutlineService } from './course-outline.service';
import { LearningService } from './learning.service';
import { COURSE, L11, L12, L21, L22, M1, M2 } from './lesson-test-data';

describe('CourseOutlineService', () => {
  const listModules = vi.fn();
  const listLessons = vi.fn();

  function load(): CourseOutline {
    TestBed.configureTestingModule({
      providers: [
        { provide: CatalogService, useValue: { getCourse: () => of(COURSE) } },
        { provide: LearningService, useValue: { listModules, listLessons } },
      ],
    });
    let result: CourseOutline | undefined;
    TestBed.inject(CourseOutlineService).load(1).subscribe((o) => (result = o));
    return result!;
  }

  beforeEach(() => {
    listModules.mockReset();
    listLessons.mockReset().mockImplementation((moduleId: number) =>
      // Returned out of order on purpose: the service must sort by displayOrder.
      of(moduleId === 10 ? [L12, L11] : [L22, L21]),
    );
  });

  it('orders modules and lessons and flattens the reading order', () => {
    listModules.mockReturnValue(of([M2, M1]));
    const outline = load();

    expect(outline.course.title).toBe(COURSE.title);
    expect(outline.modules.map((m) => m.module.id)).toEqual([10, 20]);
    expect(outline.modules[0].lessons.map((l) => l.id)).toEqual([101, 102]);
    expect(outline.lessons.map((l) => l.id)).toEqual([101, 102, 201, 202]);
  });

  it('handles a course without published module', () => {
    listModules.mockReturnValue(of([]));
    const outline = load();

    expect(outline.modules).toEqual([]);
    expect(outline.lessons).toEqual([]);
    expect(listLessons).not.toHaveBeenCalled();
  });
});
