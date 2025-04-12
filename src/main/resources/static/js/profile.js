document.addEventListener('DOMContentLoaded', function() {
    // 전역 변수
    let userInfo = null;
    const avatarModal = new bootstrap.Modal(document.getElementById('avatarModal'));
    
    // 프로필 정보 로드
    async function loadProfile() {
        try {
            const response = await fetch('/api/users/profile');
            if (!response.ok) throw new Error('프로필 정보를 불러오는데 실패했습니다.');
            
            userInfo = await response.json();
            
            // 프로필 정보 표시
            document.querySelector('.user-name').textContent = userInfo.name;
            document.querySelector('.user-email').textContent = userInfo.email;
            document.querySelector('.user-role').textContent = userInfo.role;
            document.querySelector('.user-department').textContent = userInfo.department;
            
            if (userInfo.avatarUrl) {
                document.querySelector('.user-avatar').src = userInfo.avatarUrl;
            }
            
            // 폼 필드 채우기
            document.getElementById('name').value = userInfo.name;
            document.getElementById('email').value = userInfo.email;
            document.getElementById('phone').value = userInfo.phone || '';
            document.getElementById('employeeId').value = userInfo.employeeId;
            document.getElementById('department').value = userInfo.department;
            document.getElementById('position').value = userInfo.position;
            
            // 통계 정보 로드
            loadStatistics();
            
        } catch (error) {
            showToast('error', '프로필 로드 실패', error.message);
        }
    }
    
    // 통계 정보 로드
    async function loadStatistics() {
        try {
            const response = await fetch('/api/users/statistics');
            if (!response.ok) throw new Error('통계 정보를 불러오는데 실패했습니다.');
            
            const stats = await response.json();
            
            document.getElementById('printJobCount').textContent = stats.printJobCount;
            document.getElementById('scanJobCount').textContent = stats.scanJobCount;
            document.getElementById('totalPages').textContent = stats.totalPages;
            
        } catch (error) {
            showToast('error', '통계 로드 실패', error.message);
        }
    }
    
    // 프로필 수정 폼 제출
    document.getElementById('profileForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        
        if (!this.checkValidity()) {
            e.stopPropagation();
            this.classList.add('was-validated');
            return;
        }
        
        try {
            const formData = {
                name: document.getElementById('name').value,
                email: document.getElementById('email').value,
                phone: document.getElementById('phone').value
            };
            
            const response = await fetch('/api/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });
            
            if (!response.ok) throw new Error('프로필 수정에 실패했습니다.');
            
            showToast('success', '프로필 수정 완료', '프로필 정보가 성공적으로 수정되었습니다.');
            loadProfile();
            
        } catch (error) {
            showToast('error', '프로필 수정 실패', error.message);
        }
    });
    
    // 비밀번호 변경 폼 제출
    document.getElementById('passwordForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        
        if (!this.checkValidity()) {
            e.stopPropagation();
            this.classList.add('was-validated');
            return;
        }
        
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        
        if (newPassword !== confirmPassword) {
            showToast('error', '비밀번호 불일치', '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
            return;
        }
        
        try {
            const formData = {
                currentPassword: document.getElementById('currentPassword').value,
                newPassword: newPassword
            };
            
            const response = await fetch('/api/users/password', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });
            
            if (!response.ok) throw new Error('비밀번호 변경에 실패했습니다.');
            
            showToast('success', '비밀번호 변경 완료', '비밀번호가 성공적으로 변경되었습니다.');
            this.reset();
            this.classList.remove('was-validated');
            
        } catch (error) {
            showToast('error', '비밀번호 변경 실패', error.message);
        }
    });
    
    // 프로필 이미지 변경 버튼 클릭
    document.getElementById('changeAvatar').addEventListener('click', function() {
        avatarModal.show();
    });
    
    // 이미지 파일 선택 시 미리보기
    document.getElementById('avatarFile').addEventListener('change', function() {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const preview = document.getElementById('avatarPreview');
                preview.style.display = 'block';
                preview.querySelector('img').src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    });
    
    // 프로필 이미지 저장
    document.getElementById('saveAvatar').addEventListener('click', async function() {
        const fileInput = document.getElementById('avatarFile');
        if (!fileInput.files.length) {
            showToast('error', '파일 선택', '이미지 파일을 선택해주세요.');
            return;
        }
        
        try {
            const formData = new FormData();
            formData.append('avatar', fileInput.files[0]);
            
            const response = await fetch('/api/users/avatar', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) throw new Error('프로필 이미지 변경에 실패했습니다.');
            
            showToast('success', '이미지 변경 완료', '프로필 이미지가 성공적으로 변경되었습니다.');
            avatarModal.hide();
            loadProfile();
            
        } catch (error) {
            showToast('error', '이미지 변경 실패', error.message);
        }
    });
    
    // 초기 프로필 로드
    loadProfile();
}); 