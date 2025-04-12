document.addEventListener('DOMContentLoaded', function() {
    const filterForm = document.getElementById('filterForm');
    const qrCodeList = document.getElementById('qrCodeList');
    const qrCodeTemplate = document.getElementById('qrCodeTemplate');
    const pagination = document.getElementById('pagination');
    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    let currentPage = 1;
    let totalPages = 1;
    let selectedQrCodeId = null;

    // 초기 데이터 로드
    loadQrCodes();

    // 필터 폼 제출
    filterForm.addEventListener('submit', function(e) {
        e.preventDefault();
        currentPage = 1;
        loadQrCodes();
    });

    // QR 코드 목록 로드
    async function loadQrCodes() {
        try {
            const params = new URLSearchParams({
                page: currentPage - 1,
                size: 9,
                type: document.getElementById('qrType').value,
                creator: document.getElementById('creator').value,
                startDate: document.getElementById('startDate').value,
                endDate: document.getElementById('endDate').value
            });

            const response = await fetch(`/api/qrcode/list?${params}`);
            if (!response.ok) throw new Error('QR 코드 목록을 불러오는데 실패했습니다.');
            
            const data = await response.json();
            renderQrCodes(data.content);
            renderPagination(data.totalPages);

        } catch (error) {
            console.error('QR 코드 목록 로드 오류:', error);
            showNotification('QR 코드 목록을 불러오는데 실패했습니다.', 'error');
        }
    }

    // QR 코드 렌더링
    function renderQrCodes(qrCodes) {
        qrCodeList.innerHTML = '';
        
        qrCodes.forEach(qrCode => {
            const template = qrCodeTemplate.content.cloneNode(true);
            
            // 기본 정보 설정
            template.querySelector('.qr-image').src = qrCode.imageUrl;
            template.querySelector('.qr-title').textContent = qrCode.title || '제목 없음';
            template.querySelector('.qr-creator').textContent = qrCode.creator;
            template.querySelector('.qr-date').textContent = formatDate(qrCode.createdAt);
            template.querySelector('.qr-type').textContent = getQrTypeText(qrCode.type);

            // 다운로드 버튼 이벤트
            const pngBtn = template.querySelector('.btn-download-png');
            pngBtn.addEventListener('click', () => downloadQrCode(qrCode.id, 'png'));

            const svgBtn = template.querySelector('.btn-download-svg');
            svgBtn.addEventListener('click', () => downloadQrCode(qrCode.id, 'svg'));

            // 삭제 버튼 이벤트
            const deleteBtn = template.querySelector('.btn-delete');
            deleteBtn.addEventListener('click', () => showDeleteConfirm(qrCode.id));

            qrCodeList.appendChild(template);
        });

        if (qrCodes.length === 0) {
            qrCodeList.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="mdi mdi-qrcode-remove" style="font-size: 48px; color: #ccc;"></i>
                    <p class="mt-3 text-muted">생성된 QR 코드가 없습니다.</p>
                </div>
            `;
        }
    }

    // 페이지네이션 렌더링
    function renderPagination(total) {
        totalPages = total;
        const items = [];
        
        // 이전 버튼
        items.push(`
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage - 1}">이전</a>
            </li>
        `);

        // 페이지 번호
        for (let i = 1; i <= total; i++) {
            items.push(`
                <li class="page-item ${currentPage === i ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i}</a>
                </li>
            `);
        }

        // 다음 버튼
        items.push(`
            <li class="page-item ${currentPage === total ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage + 1}">다음</a>
            </li>
        `);

        pagination.innerHTML = items.join('');

        // 페이지 클릭 이벤트
        pagination.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                const page = parseInt(this.dataset.page);
                if (page > 0 && page <= totalPages) {
                    currentPage = page;
                    loadQrCodes();
                }
            });
        });
    }

    // QR 코드 다운로드
    async function downloadQrCode(id, format) {
        try {
            const response = await fetch(`/api/qrcode/download/${id}?format=${format}`);
            if (!response.ok) throw new Error('QR 코드 다운로드에 실패했습니다.');

            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `qrcode.${format}`;
            link.click();
            URL.revokeObjectURL(url);

        } catch (error) {
            console.error('QR 코드 다운로드 오류:', error);
            showNotification('QR 코드 다운로드에 실패했습니다.', 'error');
        }
    }

    // 삭제 확인 모달 표시
    function showDeleteConfirm(id) {
        selectedQrCodeId = id;
        deleteModal.show();
    }

    // 삭제 확인
    document.getElementById('confirmDelete').addEventListener('click', async function() {
        if (!selectedQrCodeId) return;

        try {
            const response = await fetch(`/api/qrcode/${selectedQrCodeId}`, {
                method: 'DELETE'
            });

            if (!response.ok) throw new Error('QR 코드 삭제에 실패했습니다.');

            deleteModal.hide();
            showNotification('QR 코드가 삭제되었습니다.', 'success');
            loadQrCodes();

        } catch (error) {
            console.error('QR 코드 삭제 오류:', error);
            showNotification('QR 코드 삭제에 실패했습니다.', 'error');
        }
    });

    // QR 코드 유형 텍스트 변환
    function getQrTypeText(type) {
        const types = {
            'URL': 'URL',
            'TEXT': '텍스트',
            'WIFI': 'Wi-Fi 설정',
            'VCARD': '연락처',
            'EMAIL': '이메일'
        };
        return types[type] || type;
    }

    // 날짜 포맷
    function formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }
}); 