package com.mounacheikhna.rxjavaandroiddemos;

import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;

/**
 * Created by cheikhnamouna on 11/8/15.
 */
public class UnsubscribingMidStreamTest {
  public static <T> Observable.Transformer<T, T> takeNextAndUnsubscribe() {
    return observable -> {
      BehaviorSubject<T> subject = BehaviorSubject.create();
      Observable<T> source = observable.doOnNext(subject::onNext);
      return Observable
          .merge(source.takeUntil(subject), subject)
          .take(1);
    };
  }

  private Observable<Integer> doMoreWork(Integer input) {
    return  Observable.defer(() -> {
      System.out.println("Doing more work, input: " + input);
      return Observable.just(input + 50);
    });
  }

  @Test
  public void test() throws Exception {
    MyResource resource = new MyResource();
    Observable<Void> acquireResource = Observable.defer(() -> {
      resource.acquire();
      System.out.println("Acquired the resource");
      return Observable
          .<Void>never()
          .doOnUnsubscribe(() -> {
            resource.release();
            System.out.println("Released the resource");
          });
    });

    Observable<Integer> doWork = Observable.defer(() -> {
      System.out.println("Doing work");
      return Observable.just(100);
    });

    Observable<Integer> doWorkWhileAcquired =
        Observable.merge(acquireResource.cast(Integer.class), doWork);

    Observable<Integer> combinedOperations = doWorkWhileAcquired
        .compose(takeNextAndUnsubscribe())
        .flatMap(value -> acquireResource
            .cast(Integer.class)
            .mergeWith(doMoreWork(value)))
        .take(1);

    TestSubscriber<Integer> subscriber = new TestSubscriber<>();
    combinedOperations.subscribe(subscriber);
    subscriber.assertValue(150);
    subscriber.assertCompleted();
  }

  private class MyResource {

    public void acquire() {
      System.out.println("acquire \n ");
    }

    public void release() {
      System.out.println("release \n ");
    }
  }
}