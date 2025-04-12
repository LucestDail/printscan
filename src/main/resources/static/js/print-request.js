document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('printRequestForm');
    const fileInputElement = document.getElementById('fileInput');
    const fileList = document.getElementById('fileList');
    const selectedFiles = new Set();
    
    // 파일 선택 이벤트
    fileInputElement.addEventListener('change', function() {
        handleFileSelect(this.files);
    });
    
    // 드래그 앤 드롭 이벤트
    const dropZone = document.getElementById('dropZone');
    
    dropZone.addEventListener('dragover', function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.add('dragover');
    });
    
    dropZone.addEventListener('dragleave', function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.remove('dragover');
    });
    
    dropZone.addEventListener('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        this.classList.remove('dragover');
        
        const files = e.dataTransfer.files;
        handleFileSelect(files);
    });
    
    // 파일 선택 처리
    function handleFileSelect(files) {
        Array.from(files).forEach(file => {
            if (!selectedFiles.has(file.name)) {
                selectedFiles.add(file.name);
                
                const fileItem = document.createElement('div');
                fileItem.className = 'file-item';
                
                const fileName = document.createElement('span');
                fileName.textContent = file.name;
                
                const fileSize = document.createElement('span');
                fileSize.textContent = formatFileSize(file.size);
                fileSize.className = 'file-size';
                
                const removeButton = document.createElement('button');
                removeButton.innerHTML = '<i class="mdi mdi-close"></i>';
                removeButton.className = 'btn btn-sm btn-danger remove-file';
                removeButton.onclick = function() {
                    fileItem.remove();
                    selectedFiles.delete(file.name);
                };
                
                fileItem.appendChild(fileName);
                fileItem.appendChild(fileSize);
                fileItem.appendChild(removeButton);
                fileList.appendChild(fileItem);
            }
        });
    }
    
    // 파일 크기 포맷
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    // 폼 제출
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        if (!this.checkValidity()) {
            e.stopPropagation();
            this.classList.add('was-validated');
            return;
        }
        
        try {
            showLoading();
            
            const formData = new FormData(this);
            Array.from(fileInputElement.files).forEach(file => {
                formData.append('files', file);
            });
            
            const response = await fetch('/api/print/request', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) throw new Error('인쇄 요청에 실패했습니다.');
            
            showToast('success', '인쇄 요청 완료', '인쇄 요청이 성공적으로 등록되었습니다.');
            this.reset();
            fileList.innerHTML = '';
            selectedFiles.clear();
            
        } catch (error) {
            showToast('error', '인쇄 요청 실패', error.message);
        } finally {
            hideLoading();
        }
    });

    // 파일 유효성 검사
    const fileInput = document.getElementById('files');
    fileInput.addEventListener('change', function() {
        const files = Array.from(this.files);
        const maxFiles = 10;
        const maxSize = 20 * 1024 * 1024; // 20MB
        const allowedTypes = [
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-powerpoint',
            'application/vnd.openxmlformats-officedocument.presentationml.presentation'
        ];
        
        let errors = [];
        
        if (files.length > maxFiles) {
            errors.push(`파일은 최대 ${maxFiles}개까지만 첨부할 수 있습니다.`);
        }
        
        files.forEach(file => {
            if (file.size > maxSize) {
                errors.push(`${file.name}: 파일 크기는 20MB를 초과할 수 없습니다.`);
            }
            if (!allowedTypes.includes(file.type)) {
                errors.push(`${file.name}: 지원하지 않는 파일 형식입니다.`);
            }
        });
        
        if (errors.length > 0) {
            this.value = ''; // 파일 선택 초기화
            showNotification(errors.join('\n'), 'danger');
        }
    });
    
    // 긴급 우선순위 선택시 경고
    const prioritySelect = document.getElementById('priority');
    prioritySelect.addEventListener('change', function() {
        if (this.value === 'URGENT') {
            showNotification('긴급 우선순위는 관리자 승인이 필요합니다.', 'warning');
        }
    });

    // 알림 표시
    function showAlert(type, message) {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
        alertDiv.style.zIndex = '1050';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(alertDiv);

        setTimeout(() => {
            alertDiv.remove();
        }, 5000);
    }
}); 