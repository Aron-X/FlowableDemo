package com.aron;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * CallExternalSystemDelegate
 * <p></p>
 *
 * @author aron
 * @date 2019-07-16 13:59
 */
public class CallExternalSystemDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("Calling the external system for employee "
                + delegateExecution.getVariable("employee"));
    }
}
