package com.example.gps.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gps.R;

public class TermsViewerActivity extends AppCompatActivity {

    public static final String EXTRA_TERMS_TYPE = "terms_type";
    public static final int TYPE_TERMS_OF_SERVICE = 1;
    public static final int TYPE_PRIVACY_POLICY = 2;
    public static final int TYPE_LOCATION_TERMS = 3;
    public static final int TYPE_OPEN_SOURCE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_viewer);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView tvContent = findViewById(R.id.tv_terms_content);

        int termsType = getIntent().getIntExtra(EXTRA_TERMS_TYPE, TYPE_TERMS_OF_SERVICE);

        switch (termsType) {
            case TYPE_TERMS_OF_SERVICE:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("이용약관");
                }
                tvContent.setText(getTermsOfService());
                break;

            case TYPE_PRIVACY_POLICY:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("개인정보처리방침");
                }
                tvContent.setText(getPrivacyPolicy());
                break;

            case TYPE_LOCATION_TERMS:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("위치정보 이용약관");
                }
                tvContent.setText(getLocationTerms());
                break;

            case TYPE_OPEN_SOURCE:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("오픈소스 라이선스");
                }
                tvContent.setText(getOpenSourceLicenses());
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private String getTermsOfService() {
        return "WICHIN 이용약관\n\n" +
                "제1조 (목적)\n" +
                "본 약관은 WICHIN(이하 \"서비스\"라 함)의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.\n\n" +
                "제2조 (정의)\n" +
                "1. \"서비스\"란 WICHIN가 제공하는 위치 기반 친구 공유 서비스를 의미합니다.\n" +
                "2. \"이용자\"란 본 약관에 따라 회사가 제공하는 서비스를 받는 회원 및 비회원을 말합니다.\n" +
                "3. \"회원\"이란 서비스에 접속하여 본 약관에 따라 회사와 이용계약을 체결하고 회사가 제공하는 서비스를 이용하는 고객을 말합니다.\n\n" +
                "제3조 (약관의 게시와 개정)\n" +
                "1. 회사는 본 약관의 내용을 이용자가 쉽게 알 수 있도록 서비스 내 또는 연결화면을 통하여 게시합니다.\n" +
                "2. 회사는 필요한 경우 관련 법령을 위배하지 않는 범위에서 본 약관을 개정할 수 있습니다.\n" +
                "3. 회사가 약관을 개정할 경우에는 적용일자 및 개정사유를 명시하여 현행약관과 함께 서비스 내에 그 적용일자 7일 전부터 적용일자 전일까지 공지합니다.\n\n" +
                "제4조 (서비스의 제공 및 변경)\n" +
                "1. 회사는 다음과 같은 업무를 수행합니다.\n" +
                "   - 위치 기반 친구 추적 서비스 제공\n" +
                "   - 그룹 생성 및 관리 서비스\n" +
                "   - 기타 회사가 정하는 업무\n" +
                "2. 회사는 서비스의 내용 및 제공일자를 변경할 수 있으며, 변경 시 사전에 이를 공지합니다.\n\n" +
                "제5조 (서비스의 중단)\n" +
                "1. 회사는 컴퓨터 등 정보통신설비의 보수점검, 교체 및 고장, 통신의 두절 등의 사유가 발생한 경우에는 서비스의 제공을 일시적으로 중단할 수 있습니다.\n" +
                "2. 회사는 제1항의 사유로 서비스의 제공이 일시적으로 중단됨으로 인하여 이용자 또는 제3자가 입은 손해에 대하여 배상합니다. 단, 회사가 고의 또는 과실이 없음을 입증하는 경우에는 그러하지 아니합니다.\n\n" +
                "제6조 (회원가입)\n" +
                "1. 이용자는 회사가 정한 가입 양식에 따라 회원정보를 기입한 후 본 약관에 동의한다는 의사표시를 함으로써 회원가입을 신청합니다.\n" +
                "2. 회사는 제1항과 같이 회원으로 가입할 것을 신청한 이용자 중 다음 각 호에 해당하지 않는 한 회원으로 등록합니다.\n" +
                "   - 등록 내용에 허위, 기재누락, 오기가 있는 경우\n" +
                "   - 기타 회원으로 등록하는 것이 회사의 기술상 현저히 지장이 있다고 판단되는 경우\n\n" +
                "제7조 (회원 탈퇴 및 자격 상실 등)\n" +
                "1. 회원은 회사에 언제든지 탈퇴를 요청할 수 있으며 회사는 즉시 회원탈퇴를 처리합니다.\n" +
                "2. 회원이 다음 각 호의 사유에 해당하는 경우, 회사는 회원자격을 제한 및 정지시킬 수 있습니다.\n" +
                "   - 가입 신청 시에 허위 내용을 등록한 경우\n" +
                "   - 다른 사람의 서비스 이용을 방해하거나 그 정보를 도용하는 등 전자상거래 질서를 위협하는 경우\n" +
                "   - 서비스를 이용하여 법령 또는 본 약관이 금지하거나 공서양속에 반하는 행위를 하는 경우\n\n" +
                "제8조 (개인정보보호)\n" +
                "회사는 이용자의 개인정보 수집시 서비스제공을 위하여 필요한 범위에서 최소한의 개인정보를 수집합니다. 회사가 이용자의 개인정보를 수집・이용하는 때에는 당해 이용자에게 그 목적을 고지하고 동의를 받습니다.\n\n" +
                "최종 수정일: 2025년 11월 17일";
    }

    private String getPrivacyPolicy() {
        return "개인정보처리방침\n\n" +
                "WICHIN(이하 \"회사\"라 함)는 「개인정보 보호법」 제30조에 따라 정보주체의 개인정보를 보호하고 이와 관련한 고충을 신속하고 원활하게 처리할 수 있도록 하기 위하여 다음과 같이 개인정보 처리방침을 수립·공개합니다.\n\n" +
                "제1조 (개인정보의 처리 목적)\n" +
                "회사는 다음의 목적을 위하여 개인정보를 처리합니다. 처리하고 있는 개인정보는 다음의 목적 이외의 용도로는 이용되지 않으며, 이용 목적이 변경되는 경우에는 「개인정보 보호법」 제18조에 따라 별도의 동의를 받는 등 필요한 조치를 이행할 예정입니다.\n\n" +
                "1. 회원 가입 및 관리\n" +
                "   - 회원 가입의사 확인, 회원제 서비스 제공에 따른 본인 식별·인증, 회원자격 유지·관리, 서비스 부정이용 방지, 각종 고지·통지 목적으로 개인정보를 처리합니다.\n\n" +
                "2. 위치 기반 서비스 제공\n" +
                "   - 친구와의 위치 공유, 그룹 내 위치 추적, 목적지 도착 알림 등의 서비스를 제공하기 위해 위치정보를 처리합니다.\n\n" +
                "제2조 (개인정보의 처리 및 보유기간)\n" +
                "1. 회사는 법령에 따른 개인정보 보유·이용기간 또는 정보주체로부터 개인정보를 수집 시에 동의받은 개인정보 보유·이용기간 내에서 개인정보를 처리·보유합니다.\n" +
                "2. 각각의 개인정보 처리 및 보유 기간은 다음과 같습니다.\n" +
                "   - 회원 가입 및 관리: 회원 탈퇴 시까지\n" +
                "   - 위치정보: 실시간 위치 공유 종료 후 5분뒤 즉시 삭제\n\n" +
                "제3조 (처리하는 개인정보 항목)\n" +
                "회사는 다음의 개인정보 항목을 처리하고 있습니다.\n" +
                "1. 필수항목: 이메일, 비밀번호, 이름(닉네임)\n" +
                "2. 위치정보: GPS 좌표 (실시간 위치 공유 시에만 수집)\n" +
                "3. 선택항목: 프로필 사진\n\n" +
                "제4조 (개인정보의 파기)\n" +
                "1. 회사는 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 지체없이 해당 개인정보를 파기합니다.\n" +
                "2. 개인정보 파기의 절차 및 방법은 다음과 같습니다.\n" +
                "   - 파기절차: 이용자가 입력한 정보는 목적 달성 후 별도의 DB에 옮겨져 내부 방침 및 기타 관련 법령에 따라 일정기간 저장된 후 혹은 즉시 파기됩니다.\n" +
                "   - 파기방법: 전자적 파일 형태의 정보는 기록을 재생할 수 없는 기술적 방법을 사용합니다.\n\n" +
                "제5조 (정보주체의 권리·의무 및 행사방법)\n" +
                "1. 정보주체는 회사에 대해 언제든지 개인정보 열람·정정·삭제·처리정지 요구 등의 권리를 행사할 수 있습니다.\n" +
                "2. 권리 행사는 회사에 대해 「개인정보 보호법」 시행령 제41조제1항에 따라 서면, 전자우편 등을 통하여 하실 수 있으며 회사는 이에 대해 지체없이 조치하겠습니다.\n\n" +
                "제6조 (개인정보의 안전성 확보조치)\n" +
                "회사는 개인정보의 안전성 확보를 위해 다음과 같은 조치를 취하고 있습니다.\n" +
                "1. 관리적 조치: 내부관리계획 수립·시행, 정기적 직원 교육 등\n" +
                "2. 기술적 조치: 개인정보처리시스템 등의 접근권한 관리, 접근통제시스템 설치, 개인정보의 암호화 등\n" +
                "3. 물리적 조치: 전산실, 자료보관실 등의 접근통제\n\n" +
                "제7조 (개인정보 보호책임자)\n" +
                "회사는 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 정보주체의 불만처리 및 피해구제 등을 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.\n\n" +
                "개인정보 보호책임자\n" +
                "성명: WICHIN 개인정보보호팀\n" +
                "연락처: chaewoonlove2@naver.com\n\n" +
                "최종 수정일: 2025년 11월 17일";
    }

    private String getLocationTerms() {
        return "위치정보 이용약관\n\n" +
                "제1조 (목적)\n" +
                "본 약관은 WICHIN(이하 \"회사\"라 함)가 제공하는 위치기반서비스와 관련하여 회사와 개인위치정보주체와의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.\n\n" +
                "제2조 (약관 외 준칙)\n" +
                "이 약관에 명시되지 않은 사항은 위치정보의 보호 및 이용 등에 관한 법률, 개인정보보호법, 정보통신망 이용촉진 및 정보보호 등에 관한 법률, 전기통신기본법, 전기통신사업법 등 관계법령과 회사의 이용약관 및 개인정보처리방침, 회사가 별도로 정한 지침 등에 의합니다.\n\n" +
                "제3조 (서비스 내용)\n" +
                "회사는 위치정보사업자로부터 수집한 개인위치정보를 이용하여 다음과 같은 위치기반서비스를 제공합니다.\n" +
                "1. 실시간 위치 공유: 친구 또는 그룹 구성원과 실시간으로 위치를 공유하는 서비스\n" +
                "2. 위치 추적: 그룹 내에서 구성원들의 위치를 지도상에 표시하는 서비스\n" +
                "3. 목적지 도착 알림: 목적지 근처에 도착했을 때 알림을 제공하는 서비스\n" +
                "4. 경로 안내: 목적지까지의 경로를 안내하는 서비스\n\n" +
                "제4조 (개인위치정보의 수집)\n" +
                "1. 회사는 개인위치정보를 수집하기 위하여 개인위치정보주체의 동의를 받습니다.\n" +
                "2. 회사는 개인위치정보주체가 동의를 하지 않은 경우에는 개인위치정보를 수집하지 않으며, 동의를 하지 않는다는 이유로 서비스 제공을 거부하지 않습니다.\n" +
                "3. 회사는 다음 각 호의 방법으로 개인위치정보를 수집합니다.\n" +
                "   - GPS(위성위치확인시스템)를 이용한 위치정보 수집\n" +
                "   - WiFi(와이파이) 기반 위치정보 수집\n" +
                "   - 기지국(Cell ID) 기반 위치정보 수집\n\n" +
                "제5조 (개인위치정보의 이용 또는 제공)\n" +
                "1. 회사는 개인위치정보를 이용하여 서비스를 제공하고자 하는 경우에는 미리 다음 각 호의 사항을 개인위치정보주체에게 고지하고 동의를 받습니다.\n" +
                "   - 개인위치정보 이용·제공 목적\n" +
                "   - 제공받는 자\n" +
                "   - 제공하는 개인위치정보의 항목\n" +
                "2. 회사는 개인위치정보를 개인위치정보주체가 지정하는 제3자에게 제공하는 경우에는 개인위치정보를 수집한 당해 통신 단말장치로 매회 개인위치정보주체에게 제공받는 자, 제공일시 및 제공목적을 즉시 통보합니다.\n\n" +
                "제6조 (개인위치정보의 보유 및 파기)\n" +
                "1. 회사는 위치기반서비스 제공을 위해 필요한 최소한의 기간 동안만 개인위치정보를 보유합니다.\n" +
                "2. 실시간 위치 공유 서비스의 경우, 위치 공유가 종료되는 즉시 개인위치정보를 파기합니다.\n" +
                "3. 다만, 다음 각 호의 경우에는 예외로 합니다.\n" +
                "   - 다른 법령에 따라 보유하여야 하는 경우\n" +
                "   - 개인위치정보주체의 동의를 받은 경우\n\n" +
                "제7조 (개인위치정보주체의 권리)\n" +
                "1. 개인위치정보주체는 개인위치정보의 수집·이용·제공에 대한 동의의 전부 또는 일부를 언제든지 철회할 수 있습니다.\n" +
                "2. 개인위치정보주체는 언제든지 개인위치정보의 수집·이용·제공의 일시적인 중지를 요구할 수 있습니다. 이 경우 회사는 즉시 이를 중지하고, 이를 거부하지 아니합니다.\n" +
                "3. 개인위치정보주체는 회사에 대하여 다음 각 호의 자료에 대한 열람 또는 고지를 요구할 수 있고, 당해 자료에 오류가 있는 경우에는 그 정정을 요구할 수 있습니다.\n" +
                "   - 개인위치정보주체에 대한 위치정보 수집·이용·제공사실 확인자료\n" +
                "   - 개인위치정보주체의 개인위치정보가 위치정보의 보호 및 이용 등에 관한 법률 또는 다른 법령의 규정에 의하여 제3자에게 제공된 이유 및 내용\n\n" +
                "제8조 (법정대리인의 권리)\n" +
                "1. 회사는 만 14세 미만 아동으로부터 개인위치정보를 수집·이용 또는 제공하고자 하는 경우에는 만 14세 미만 아동과 그 법정대리인의 동의를 받아야 합니다.\n" +
                "2. 법정대리인은 만 14세 미만 아동의 개인위치정보 수집·이용·제공에 동의하는 경우 동의유보권을 행사할 수 있으며, 개인위치정보주체의 권리를 모두 행사할 수 있습니다.\n\n" +
                "제9조 (8세 이하의 아동 등의 보호의무자의 권리)\n" +
                "1. 회사는 아래의 경우에 해당하는 자(이하 \"8세 이하의 아동 등\"이라 합니다)의 보호의무자가 8세 이하의 아동 등의 생명 또는 신체보호를 위하여 개인위치정보의 이용 또는 제공에 동의하는 경우에는 본인의 동의가 있는 것으로 봅니다.\n" +
                "   - 8세 이하의 아동\n" +
                "   - 피성년후견인\n" +
                "   - 장애인복지법 제2조제2항제2호의 규정에 의한 정신적 장애를 가진 자로서 장애인고용촉진 및 직업재활법 제2조제2호의 규정에 의한 중증장애인에 해당하는 자(장애인복지법 제32조의 규정에 의하여 장애인등록을 한 자에 한합니다)\n" +
                "2. 8세 이하의 아동 등의 생명 또는 신체의 보호를 위하여 개인위치정보의 이용 또는 제공에 동의를 하고자 하는 보호의무자는 서면동의서에 보호의무자임을 증명하는 서면을 첨부하여 회사에 제출하여야 합니다.\n\n" +
                "제10조 (위치정보관리책임자의 지정)\n" +
                "회사는 위치정보를 적절히 관리·보호하고 개인위치정보주체의 불만을 원활히 처리할 수 있도록 실질적인 책임을 질 수 있는 지위에 있는 자를 위치정보관리책임자로 지정합니다.\n\n" +
                "위치정보관리책임자\n" +
                "성명: WICHIN 위치정보관리팀\n" +
                "연락처: chaewoonlove2@naver.com\n\n" +
                "최종 수정일: 2025년 11월 17일";
    }

    private String getOpenSourceLicenses() {
        return "오픈소스 라이선스\n\n" +
                "WICHIN는 다음의 오픈소스 라이브러리를 사용합니다.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "1. Naver Maps Android SDK\n" +
                "Copyright (c) NAVER Corp.\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "2. Retrofit\n" +
                "Copyright 2013 Square, Inc.\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "3. Gson\n" +
                "Copyright 2008 Google Inc.\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "4. OkHttp\n" +
                "Copyright 2019 Square, Inc.\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "5. Glide\n" +
                "Copyright 2014 Google, Inc. All rights reserved.\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "6. Firebase Android SDK\n" +
                "Copyright (c) Firebase\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "7. Material Components for Android\n" +
                "Copyright (c) Google Inc.\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "8. AndroidX Libraries\n" +
                "Copyright (c) The Android Open Source Project\n" +
                "Licensed under the Apache License, Version 2.0\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "Apache License, Version 2.0\n\n" +
                "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                "you may not use this file except in compliance with the License.\n" +
                "You may obtain a copy of the License at\n\n" +
                "    http://www.apache.org/licenses/LICENSE-2.0\n\n" +
                "Unless required by applicable law or agreed to in writing, software\n" +
                "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "See the License for the specific language governing permissions and\n" +
                "limitations under the License.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "이 외에도 다양한 오픈소스 라이브러리가 사용되었으며, 각 라이브러리의 라이선스는 해당 라이브러리의 공식 문서를 참조하시기 바랍니다.\n\n" +
                "최종 수정일: 2025년 11월 17일";
    }
}

