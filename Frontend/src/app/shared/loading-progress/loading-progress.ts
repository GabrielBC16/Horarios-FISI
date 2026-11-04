import { Component, Input, OnChanges, OnDestroy, computed, signal } from '@angular/core';

export interface ProgressStage {
  afterSeconds: number;
  label: string;
  detail: string;
}

@Component({
  selector: 'app-loading-progress',
  templateUrl: './loading-progress.html',
  styleUrl: './loading-progress.scss',
})
export class LoadingProgressComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) active = false;
  @Input({ required: true }) title = '';
  @Input({ required: true }) description = '';
  @Input() stages: ProgressStage[] = [];

  readonly elapsedSeconds = signal(0);
  readonly currentStage = computed(() => {
    const elapsed = this.elapsedSeconds();
    return this.stages.reduce((current, stage, index) =>
      stage.afterSeconds <= elapsed ? index : current, 0);
  });

  private intervalId?: number;

  ngOnChanges() {
    if (this.active) {
      this.startTimer();
    } else {
      this.stopTimer();
    }
  }

  ngOnDestroy() {
    this.stopTimer();
  }

  isComplete(index: number) {
    return index < this.currentStage();
  }

  isCurrent(index: number) {
    return index === this.currentStage();
  }

  elapsedLabel() {
    const elapsed = this.elapsedSeconds();
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;
    return minutes ? `${minutes} min ${seconds.toString().padStart(2, '0')} s` : `${seconds} s`;
  }

  private startTimer() {
    this.stopTimer();
    this.elapsedSeconds.set(0);
    this.intervalId = window.setInterval(() => this.elapsedSeconds.update((value) => value + 1), 1000);
  }

  private stopTimer() {
    if (this.intervalId !== undefined) {
      window.clearInterval(this.intervalId);
      this.intervalId = undefined;
    }
  }
}
