package com.cars24.taskmanagement.backend.constants;



import java.util.*;

public class SubModuleTaskMapping {
    public static final Map<String, Set<String>> SUBMODULE_TASK_MAP = new HashMap<>();

    static {
        SUBMODULE_TASK_MAP.put("BASIC_DETAILS", new HashSet<>(Arrays.asList(
                "consent_send_otp", "consent_verify_otp", "pan_validation", "basic_detail_submit",
                "dob_check", "ogl_check", "fraud_check", "initiate_bajaj_offer", "upload_pan_photo"
        )));

        SUBMODULE_TASK_MAP.put("ADDRESS", new HashSet<>(Arrays.asList(
                "current_address_capture", "update_co_applicant_address"
        )));

        SUBMODULE_TASK_MAP.put("ADDITIONAL_DETAILS", new HashSet<>(Arrays.asList(
                "additional_contact_detail_capture", "additional_address_detail_capture",
                "personal_detail_capture", "employment_detail_capture", "pre_credit",
                "upsert_co_applicant_employment", "update_co_applicant_contact",
                "update_co_applicant_additional_personal_details"
        )));

        SUBMODULE_TASK_MAP.put("CO_APPLICANT", new HashSet<>(Arrays.asList(
                "send_co_applicant_consent", "verify_consent_and_add_co_applicant", "remove_co_applicant",
                "add_co_applicant", "co_applicant_mandatory", "co_app_fraud_check",
                "co_applicant_dob_check", "co_applicant_ogl_check", "co_applicant_pan_validation",
                "co_applicant_upload_pan_photo", "co_applicant_tvr_escalate_to_manager",
                "co_applicant_tvr_approve", "co_applicant_tvr_reject", "co_applicant_dc_approve",
                "co_applicant_dc_reject", "co_applicant_dc_escalate_to_manager"
        )));

        SUBMODULE_TASK_MAP.put("BANKING", new HashSet<>(Arrays.asList(
                "banking_mandatory", "banking_optional", "banking_manual_sufficiency_check",
                "banking_initiate_txn", "banking_upload_statement", "banking_process_statement",
                "banking_initiate_report_generation", "banking_validation", "banking_sufficiency_check",
                "co_app_banking_mandatory", "co_app_banking_optional", "co_app_banking_manual_sufficiency_check",
                "co_app_banking_initiate_txn", "co_app_banking_upload_statement", "co_app_banking_process_statement",
                "co_app_banking_initiate_report_generation", "co_app_banking_validation", "co_app_banking_sufficiency_check"
        )));

        SUBMODULE_TASK_MAP.put("OFFERS", new HashSet<>(Arrays.asList(
                "initiate_offer_approval", "terms_generation", "check_partner_eligibility"
        )));

        SUBMODULE_TASK_MAP.put("OFFER_APPROVAL", new HashSet<>(Arrays.asList(
                "manager_approval"
        )));

        SUBMODULE_TASK_MAP.put("TNC_DETAILS", new HashSet<>(Arrays.asList(
                "action_on_terms", "disburse_to_partner"
        )));

        SUBMODULE_TASK_MAP.put("DOCUMENTS", new HashSet<>(Arrays.asList(
                "doc_upload", "additional_info"
        )));

        SUBMODULE_TASK_MAP.put("BENEFICIARY_DETAILS", new HashSet<>(Arrays.asList(
                "add_beneficiary_details"
        )));

        SUBMODULE_TASK_MAP.put("TVR", new HashSet<>(Arrays.asList(
                "tvr_escalate_to_manager", "tvr_approve", "tvr_reject", "tvr_offer_approve",
                "tvr_offer_reject", "tvr_offer_escalate_to_manager"
        )));

        SUBMODULE_TASK_MAP.put("DILIGENCE", new HashSet<>(Arrays.asList(
                "dc_approve", "dc_reject", "dc_escalate_to_manager"
        )));

        SUBMODULE_TASK_MAP.put("ASSETS", new HashSet<>(Arrays.asList(
                "start_credit_workflow", "refresh_bookings_and_attach_assets", "cibil_pull",
                "co_applicant_cibil_pull", "cars24_cibil_based_offer", "add_asset", "add_asset_c2c"
        )));

        SUBMODULE_TASK_MAP.put("BOOKING_TRANSFER", new HashSet<>(Arrays.asList(
                "detach_asset_c2c", "booking_transfer"
        )));

        SUBMODULE_TASK_MAP.put("PA_OFFER", new HashSet<>(Arrays.asList(
                "pa_offer"
        )));

        SUBMODULE_TASK_MAP.put("KYC", new HashSet<>(Arrays.asList(
                "fulfilment_kyc", "fulfilment_kyc_webhook"
        )));

        SUBMODULE_TASK_MAP.put("NACH", new HashSet<>(Arrays.asList(
                "fulfilment_nach", "fulfilment_nach_webhook"
        )));

        SUBMODULE_TASK_MAP.put("LEGALITY_AGREEMENT", new HashSet<>(Arrays.asList(
                "fulfilment_agreement", "fulfilment_agreement_webhook"
        )));

        SUBMODULE_TASK_MAP.put("REFERENCES", new HashSet<>(Arrays.asList(
                "fulfilment_reference_call", "fulfilment_reference_call_webhook"
        )));

        SUBMODULE_TASK_MAP.put("CHM_INSTALLATION", new HashSet<>(Arrays.asList(
                "chm_install_initiate"
        )));

        SUBMODULE_TASK_MAP.put("DISBURSAL_CONFIRMATION", new HashSet<>(Arrays.asList(
                "tranche_aggregation"
        )));

        SUBMODULE_TASK_MAP.put("FCU", new HashSet<>(Arrays.asList(
                "fcu_checks"
        )));

        SUBMODULE_TASK_MAP.put("RCU", new HashSet<>(Arrays.asList(
                "rcu_checks"
        )));

        SUBMODULE_TASK_MAP.put("RTO_KIT_APPROVAL", new HashSet<>(Arrays.asList(
                "rto_kit_approval"
        )));

        SUBMODULE_TASK_MAP.put("QC", new HashSet<>(Arrays.asList(
                "final_qc"
        )));

        SUBMODULE_TASK_MAP.put("RTO", new HashSet<>(Arrays.asList(
                "rto_completion"
        )));

        SUBMODULE_TASK_MAP.put("PARTNER_LOGIN_APPROVAL", new HashSet<>(Arrays.asList(
                "partner_action_on_proposal"
        )));

        SUBMODULE_TASK_MAP.put("SENDBACK", new HashSet<>(Arrays.asList(
                "sendback"
        )));

        SUBMODULE_TASK_MAP.put("INIT_CREDIT_FLOW", new HashSet<>(Arrays.asList(
                "init_credit_flow"
        )));

        SUBMODULE_TASK_MAP.put("ENHANCE_OFFER", new HashSet<>(Arrays.asList(
                "co_doc_upload","initiate_offer_approval","terms_generation", "co_action_on_terms"
                ,"co_beneficiary", "additional"
        )));
    }


}
