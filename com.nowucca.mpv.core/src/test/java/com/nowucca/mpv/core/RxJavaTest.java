package com.nowucca.mpv.core;

import org.junit.Test;
import rx.Observable;

/**
 */
public class RxJavaTest {



    @Test
    public void canZipSourceAndDerivedStream() throws Exception {
        Observable<Integer> generator = Observable.range(100, 100);

        Observable<Integer> generator2 = Observable.range(200, 100);


        System.out.print("generator.mergeWith: ");
        generator
                .mergeWith(generator2)
                .doOnCompleted(()->System.out.println())
                .subscribe((i) -> System.out.format("%3d ", i));

    }
}
