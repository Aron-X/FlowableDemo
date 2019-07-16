package com.aron;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * SendRejectionMail
 * <p></p>
 *
 * @author aron
 * @date 2019-07-16 13:59
 */
public class SendRejectionMail implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("Send out rejection email for employee "
                + delegateExecution.getVariable("employee"));
    }
}
