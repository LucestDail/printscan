document.addEventListener('DOMContentLoaded', function() {
    // 설정 로드
    loadSettings();
    
    // 시스템 정보 로드
    loadSystemInfo();
    
    // 다크 모드 스위치 이벤트
    document.getElementById('darkModeSwitch').addEventListener('change', function() {
        toggleTheme();
        saveSettings();
    });
    
    // 설정 저장 버튼 이벤트
    document.getElementById('saveSettings').addEventListener('click', saveSettings);
    
    // 설정 변경 이벤트
    const settingInputs = document.querySelectorAll('select, input[type="checkbox"]');
    settingInputs.forEach(input => {
        input.addEventListener('change', function() {
            if (this.id !== 'darkModeSwitch') {
                saveSettings();
            }
        });
    });
});

// 설정 로드
function loadSettings() {
    // 로컬 스토리지에서 설정 로드
    const settings = JSON.parse(localStorage.getItem('settings') || '{}');
    
    // 다크 모드 설정
    document.getElementById('darkModeSwitch').checked = settings.darkMode || false;
    
    // 알림 설정
    document.getElementById('emailNotification').checked = settings.emailNotification || false;
    document.getElementById('desktopNotification').checked = settings.desktopNotification || false;
    
    // 언어 설정
    document.getElementById('languageSelect').value = settings.language || 'ko';
    
    // 시간대 설정
    document.getElementById('timezoneSelect').value = settings.timezone || 'Asia/Seoul';
    
    // 데이터 표시 설정
    document.getElementById('itemsPerPage').value = settings.itemsPerPage || '20';
    document.getElementById('dateFormat').value = settings.dateFormat || 'YYYY-MM-DD';
}

// 설정 저장
async function saveSettings() {
    const settings = {
        darkMode: document.getElementById('darkModeSwitch').checked,
        emailNotification: document.getElementById('emailNotification').checked,
        desktopNotification: document.getElementById('desktopNotification').checked,
        language: document.getElementById('languageSelect').value,
        timezone: document.getElementById('timezoneSelect').value,
        itemsPerPage: document.getElementById('itemsPerPage').value,
        dateFormat: document.getElementById('dateFormat').value
    };
    
    try {
        // 서버에 설정 저장
        const response = await fetch('/api/users/settings', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(settings)
        });
        
        if (!response.ok) throw new Error('설정 저장에 실패했습니다.');
        
        // 로컬 스토리지에 설정 저장
        localStorage.setItem('settings', JSON.stringify(settings));
        
        showToast('success', '설정 저장 완료', '설정이 성공적으로 저장되었습니다.');
        
    } catch (error) {
        showToast('error', '설정 저장 실패', error.message);
    }
}

// 시스템 정보 로드
function loadSystemInfo() {
    // 브라우저 정보
    const browserInfo = document.getElementById('browserInfo');
    browserInfo.textContent = navigator.userAgent;
    
    // 운영체제 정보
    const osInfo = document.getElementById('osInfo');
    const os = navigator.platform || navigator.userAgentData?.platform || 'Unknown';
    osInfo.textContent = os;
} 