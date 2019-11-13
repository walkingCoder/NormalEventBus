package com.beijing.zzu.normalBroadcastReceiver;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;

import com.google.common.collect.Maps;
import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import java.util.concurrent.ConcurrentMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * @author jiayk
 * @date 2019/11/11
 */
public class RxEventBus {
    public interface EventCallback<T> {
        /**
         * Function that consumes a event of T
         *
         * @param event
         */
        void onEvent(T event);
    }

    private ConcurrentMap<Class, Subject> subjectMap;

    private static volatile RxEventBus instance;

    private RxEventBus() {
        subjectMap = Maps.newConcurrentMap();
    }

    public static RxEventBus getInstance() {
        if (instance == null) {
            synchronized (RxEventBus.class) {
                if (instance == null) {
                    instance = new RxEventBus();
                }
            }
        }

        return instance;
    }

    /**
     * Just for unfolding generic type.
     * Workaround for type inferring.
     *
     * @param <S> the delegated generic type.
     */
    public class Bus<S> {
        private final Class<S> eventIndex;

        private Bus(Class<S> event) {
            this.eventIndex = event;
        }

        public Disposable listen(final EventCallback<S> callback) {
            Subject<S> subject = subjectMap.get(eventIndex);
            if (subject == null) {
                subject = PublishSubject.create();
                subjectMap.put(eventIndex, subject);
            }
            return subject.observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<S>() {
                @Override
                public void accept(S o) throws Exception {
                    callback.onEvent(o);
                }
            });
        }


        public Disposable listen(final EventCallback<S> callback, LifecycleOwner owner, Lifecycle.Event event) {
            Subject<S> subject = subjectMap.get(eventIndex);
            if (subject == null) {
                subject = PublishSubject.create();
                subjectMap.put(eventIndex, subject);
            }
            return subject.observeOn(AndroidSchedulers.mainThread())
                    .as(AutoDispose.<S>autoDisposable(AndroidLifecycleScopeProvider.from(owner, event)))
                    .subscribe(new Consumer<S>() {
                        @Override
                        public void accept(S o) throws Exception {
                            callback.onEvent(o);
                        }
                    });
        }

        public Disposable listen(final EventCallback<S> callback, LifecycleOwner owner) {
            Subject<S> subject = subjectMap.get(eventIndex);
            if (subject == null) {
                subject = PublishSubject.create();
                subjectMap.put(eventIndex, subject);
            }
            return subject.observeOn(AndroidSchedulers.mainThread())
                    .as(AutoDispose.<S>autoDisposable(AndroidLifecycleScopeProvider.from(owner)))
                    .subscribe(new Consumer<S>() {
                        @Override
                        public void accept(S o) throws Exception {
                            callback.onEvent(o);
                        }
                    });
        }
    }

    public <T> Bus<T> on(Class<T> clazz) {
        return new Bus<>(clazz);
    }


    public void fire(Object object) {
        Subject subject = subjectMap.get(object.getClass());
        if (subject == null) {
            return;
        }
        subject.onNext(object);
    }

    public void clear(Class eventClazz) {
        subjectMap.remove(eventClazz);
    }
}
