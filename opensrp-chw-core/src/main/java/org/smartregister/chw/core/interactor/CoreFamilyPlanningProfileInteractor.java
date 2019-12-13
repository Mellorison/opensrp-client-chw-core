package org.smartregister.chw.core.interactor;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.dao.PNCDao;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.contract.BaseFpProfileContract;
import org.smartregister.chw.fp.domain.FpMemberObject;
import org.smartregister.chw.fp.interactor.BaseFpProfileInteractor;
import org.smartregister.domain.AlertStatus;

import java.util.Date;

public class CoreFamilyPlanningProfileInteractor extends BaseFpProfileInteractor {

    @Override
    public void updateProfileFpStatusInfo(FpMemberObject memberObject, BaseFpProfileContract.InteractorCallback callback) {
        Runnable runnable = new Runnable() {

            Date lastVisitDate = getLastVisitDate(memberObject);

            @Override
            public void run() {
                appExecutors.mainThread().execute(() -> {
                    callback.refreshFamilyStatus(AlertStatus.normal);
                    callback.refreshLastVisit(lastVisitDate);
                    callback.refreshUpComingServicesStatus("Family Planning Followup Visit", AlertStatus.normal, new Date());
                    callback.refreshMedicalHistory(true);
                });
            }
        };
        appExecutors.diskIO().execute(runnable);
    }

    private String getMemberVisitType(String baseEntityId) {
        String type = null;

        if (AncDao.isANCMember(baseEntityId)) {
            type = CoreConstants.TABLE_NAME.ANC_MEMBER;
        } else if (PNCDao.isPNCMember(baseEntityId)) {
            type = CoreConstants.TABLE_NAME.PNC_MEMBER;
        }
        return type;
    }

    private Date getLastVisitDate(FpMemberObject memberObject) {
        Date lastVisitDate = null;
        String visitType = null;
        String memberType = getMemberVisitType(memberObject.getBaseEntityId());

        switch (memberType) {
            case CoreConstants.TABLE_NAME.PNC_MEMBER:
                visitType = Constants.EVENT_TYPE.PNC_HOME_VISIT;
                break;
            case CoreConstants.TABLE_NAME.ANC_MEMBER:
                visitType = Constants.EVENT_TYPE.ANC_HOME_VISIT;
                break;
            default:
                break;
        }

        if (StringUtils.isNotBlank(visitType)) {
            Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), visitType);
            if (lastVisit != null) {
                lastVisitDate = lastVisit.getDate();
            }
        }

        return lastVisitDate;
    }

}