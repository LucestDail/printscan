document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('qrGenerateForm');
    const qrType = document.getElementById('qrType');
    const qrPreview = document.getElementById('qrPreview');
    const downloadPngBtn = document.getElementById('downloadPng');
    const downloadSvgBtn = document.getElementById('downloadSvg');
    let currentQrData = null;

    // QR 코드 유형 변경 시 해당하는 폼 표시
    qrType.addEventListener('change', function() {
        // 모든 입력 폼 숨기기
        document.querySelectorAll('.qr-input-form').forEach(form => {
            form.style.display = 'none';
        });

        // 선택된 유형의 폼 표시
        const selectedForm = document.getElementById(`${this.value.toLowerCase()}Form`);
        if (selectedForm) {
            selectedForm.style.display = 'block';
        }
    });

    // 폼 제출 처리
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        if (!form.checkValidity()) {
            e.stopPropagation();
            form.classList.add('was-validated');
            return;
        }

        try {
            const qrData = generateQrData();
            if (!qrData) {
                showNotification('QR 코드 데이터를 생성할 수 없습니다.', 'error');
                return;
            }

            currentQrData = qrData;
            await generateQrCode(qrData);
            
            // 다운로드 버튼 활성화
            downloadPngBtn.disabled = false;
            downloadSvgBtn.disabled = false;

        } catch (error) {
            console.error('QR 코드 생성 오류:', error);
            showNotification('QR 코드 생성 중 오류가 발생했습니다.', 'error');
        }
    });

    // QR 코드 데이터 생성
    function generateQrData() {
        const type = qrType.value;
        let data = '';

        switch (type) {
            case 'URL':
                data = document.getElementById('url').value;
                break;

            case 'TEXT':
                data = document.getElementById('text').value;
                break;

            case 'WIFI':
                const ssid = document.getElementById('ssid').value;
                const password = document.getElementById('password').value;
                const encryption = document.getElementById('encryption').value;
                data = `WIFI:T:${encryption};S:${ssid};P:${password};;`;
                break;

            case 'VCARD':
                const name = document.getElementById('name').value;
                const org = document.getElementById('organization').value;
                const title = document.getElementById('title').value;
                const phone = document.getElementById('phone').value;
                const email = document.getElementById('email').value;
                const address = document.getElementById('address').value;

                data = 'BEGIN:VCARD\n' +
                       'VERSION:3.0\n' +
                       `FN:${name}\n` +
                       (org ? `ORG:${org}\n` : '') +
                       (title ? `TITLE:${title}\n` : '') +
                       (phone ? `TEL:${phone}\n` : '') +
                       (email ? `EMAIL:${email}\n` : '') +
                       (address ? `ADR:;;${address};;;;\n` : '') +
                       'END:VCARD';
                break;

            case 'EMAIL':
                const to = document.getElementById('emailTo').value;
                const subject = document.getElementById('emailSubject').value;
                const body = document.getElementById('emailBody').value;
                data = `mailto:${to}`;
                if (subject || body) {
                    data += '?';
                    if (subject) data += `subject=${encodeURIComponent(subject)}`;
                    if (subject && body) data += '&';
                    if (body) data += `body=${encodeURIComponent(body)}`;
                }
                break;

            default:
                return null;
        }

        return data;
    }

    // QR 코드 생성
    async function generateQrCode(data) {
        const size = parseInt(document.getElementById('size').value);
        const errorCorrection = document.getElementById('errorCorrection').value;
        const margin = document.getElementById('margin').checked ? 4 : 0;

        const options = {
            errorCorrectionLevel: errorCorrection,
            margin: margin,
            width: size,
            height: size
        };

        // Canvas에 QR 코드 생성
        await QRCode.toCanvas(qrPreview, data, options);
    }

    // PNG 다운로드
    downloadPngBtn.addEventListener('click', function() {
        if (!currentQrData) return;

        const canvas = qrPreview;
        const link = document.createElement('a');
        link.download = 'qrcode.png';
        link.href = canvas.toDataURL('image/png');
        link.click();
    });

    // SVG 다운로드
    downloadSvgBtn.addEventListener('click', async function() {
        if (!currentQrData) return;

        try {
            const size = parseInt(document.getElementById('size').value);
            const errorCorrection = document.getElementById('errorCorrection').value;
            const margin = document.getElementById('margin').checked ? 4 : 0;

            const options = {
                errorCorrectionLevel: errorCorrection,
                margin: margin,
                width: size,
                height: size,
                type: 'svg'
            };

            // SVG 문자열 생성
            const svgString = await QRCode.toString(currentQrData, options);

            // Blob 생성 및 다운로드
            const blob = new Blob([svgString], { type: 'image/svg+xml' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.download = 'qrcode.svg';
            link.href = url;
            link.click();
            URL.revokeObjectURL(url);

        } catch (error) {
            console.error('SVG 다운로드 오류:', error);
            showNotification('SVG 파일 생성 중 오류가 발생했습니다.', 'error');
        }
    });
}); 