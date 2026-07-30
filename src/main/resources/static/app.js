(() => {
  const STORAGE_KEYS = {
    resume: 'rm:resume',
    jobDescription: 'rm:jobDescription',
    versions: 'rm:versions',
    chat: 'rm:chat',
    provider: 'rm:provider'
  };

  const resumeInput = document.getElementById('resume-input');
  const jdInput = document.getElementById('jd-input');
  const savedIndicator = document.getElementById('resume-saved-indicator');
  const resumeFileInput = document.getElementById('resume-file-input');
  const resumeFileBtn = document.getElementById('resume-file-btn');
  const resumeFileName = document.getElementById('resume-file-name');
  const versionList = document.getElementById('version-list');
  const clearVersionsBtn = document.getElementById('clear-versions-btn');
  const copyBtn = document.getElementById('copy-btn');
  const downloadBtn = document.getElementById('download-btn');
  const providerSelect = document.getElementById('provider-select');
  const chatMessagesEl = document.getElementById('chat-messages');
  const chatForm = document.getElementById('chat-form');
  const chatInput = document.getElementById('chat-input');
  const chatSendBtn = document.getElementById('chat-send-btn');
  const appLayout = document.getElementById('app-layout');
  const previewPanel = document.getElementById('preview-panel');
  const previewText = document.getElementById('preview-text');
  const previewCloseBtn = document.getElementById('preview-close-btn');
  const previewApplyBtn = document.getElementById('preview-apply-btn');
  const previewCopyBtn = document.getElementById('preview-copy-btn');
  const previewDownloadBtn = document.getElementById('preview-download-btn');
  const loadingModal = document.getElementById('loading-modal');
  const loadingModalText = document.getElementById('loading-modal-text');

  function showLoadingModal(text) {
    loadingModalText.textContent = text;
    loadingModal.hidden = false;
  }

  function hideLoadingModal() {
    loadingModal.hidden = true;
  }

  let versions = loadJson(STORAGE_KEYS.versions, []);
  let chatHistory = loadJson(STORAGE_KEYS.chat, []);
  let previewIndex = null;

  function loadJson(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) {
      return fallback;
    }
  }

  function saveJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function initFields() {
    resumeInput.value = localStorage.getItem(STORAGE_KEYS.resume) || '';
    jdInput.value = localStorage.getItem(STORAGE_KEYS.jobDescription) || '';
    providerSelect.value = localStorage.getItem(STORAGE_KEYS.provider) || 'claude';
  }

  providerSelect.addEventListener('change', () => {
    localStorage.setItem(STORAGE_KEYS.provider, providerSelect.value);
  });

  let saveTimer = null;
  function debouncedSave(key, value, indicatorEl) {
    localStorage.setItem(key, value);
    if (indicatorEl) {
      indicatorEl.textContent = '저장됨';
      indicatorEl.classList.add('visible');
      clearTimeout(saveTimer);
      saveTimer = setTimeout(() => indicatorEl.classList.remove('visible'), 1200);
    }
  }

  resumeInput.addEventListener('input', () => {
    debouncedSave(STORAGE_KEYS.resume, resumeInput.value, savedIndicator);
  });
  jdInput.addEventListener('input', () => {
    localStorage.setItem(STORAGE_KEYS.jobDescription, jdInput.value);
  });

  resumeFileBtn.addEventListener('click', () => resumeFileInput.click());

  resumeFileInput.addEventListener('change', async () => {
    const file = resumeFileInput.files[0];
    if (!file) return;

    resumeFileName.textContent = file.name;
    const ext = file.name.split('.').pop().toLowerCase();
    resumeFileBtn.disabled = true;

    try {
      let text;
      if (ext === 'pdf') {
        resumeFileBtn.textContent = '추출 중...';
        text = await extractPdfText(file);
      } else {
        text = await readFileAsText(file);
      }
      resumeInput.value = text;
      debouncedSave(STORAGE_KEYS.resume, text, savedIndicator);
    } catch (err) {
      resumeFileName.textContent = '';
      alert(`파일을 읽지 못했습니다: ${err.message}`);
    } finally {
      resumeFileBtn.disabled = false;
      resumeFileBtn.textContent = '파일 업로드 (.txt, .md, .pdf)';
      resumeFileInput.value = '';
    }
  });

  function readFileAsText(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = () => reject(reader.error || new Error('파일을 읽을 수 없습니다.'));
      reader.readAsText(file);
    });
  }

  async function extractPdfText(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await fetch('/api/resume/extract', { method: 'POST', body: formData });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.message || `서버 오류 (${response.status})`);
    }
    return data.text;
  }

  function renderVersions() {
    versionList.innerHTML = '';
    if (versions.length === 0) {
      const li = document.createElement('li');
      li.className = 'version-empty';
      li.textContent = '아직 저장된 버전이 없습니다';
      versionList.appendChild(li);
      return;
    }
    [...versions].reverse().forEach((version) => {
      const li = document.createElement('li');

      const meta = document.createElement('span');
      meta.className = 'version-meta';
      meta.textContent = `${formatTime(version.createdAt)} · ${version.text.slice(0, 40).replace(/\s+/g, ' ')}...`;
      li.appendChild(meta);

      const actions = document.createElement('span');

      const restoreBtn = document.createElement('button');
      restoreBtn.textContent = '복원';
      restoreBtn.addEventListener('click', () => {
        resumeInput.value = version.text;
        localStorage.setItem(STORAGE_KEYS.resume, version.text);
      });
      actions.appendChild(restoreBtn);

      li.appendChild(actions);
      versionList.appendChild(li);
    });
  }

  function formatTime(iso) {
    const d = new Date(iso);
    return d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  function pushVersion(text) {
    versions.push({ id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, createdAt: new Date().toISOString(), text });
    saveJson(STORAGE_KEYS.versions, versions);
    renderVersions();
  }

  clearVersionsBtn.addEventListener('click', () => {
    if (!confirm('저장된 모든 버전을 삭제할까요?')) return;
    versions = [];
    saveJson(STORAGE_KEYS.versions, versions);
    renderVersions();
  });

  copyBtn.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(resumeInput.value);
      flashButton(copyBtn, '복사됨!');
    } catch (e) {
      flashButton(copyBtn, '복사 실패');
    }
  });

  downloadBtn.addEventListener('click', () => {
    downloadResumeAsPdf(resumeInput.value, downloadBtn);
  });

  async function downloadResumeAsPdf(text, triggerBtn) {
    if (!text.trim()) {
      alert('다운로드할 이력서 내용이 없습니다.');
      return;
    }

    triggerBtn.disabled = true;
    showLoadingModal('AI가 이력서를 PDF 템플릿에 맞춰 변환하고 있습니다...');

    try {
      const response = await fetch('/api/resume/format', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resume: text, provider: providerSelect.value })
      });

      if (!response.ok) {
        throw new Error(`서버 오류 (${response.status})`);
      }

      const data = await response.json();
      openResumePrintWindow(data.html);
    } catch (err) {
      alert(`PDF 포맷 변환에 실패했습니다: ${err.message}`);
    } finally {
      triggerBtn.disabled = false;
      hideLoadingModal();
    }
  }

  function openResumePrintWindow(bodyHtml) {
    const printWindow = window.open('', '_blank');
    if (!printWindow) {
      alert('팝업이 차단되어 미리보기를 열 수 없습니다. 브라우저의 팝업 차단을 해제해주세요.');
      return;
    }

    printWindow.document.write(`<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>이력서</title>
<style>
  @page { margin: 20mm 18mm; }
  body { margin: 0; }
  .resume {
    font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Malgun Gothic', -apple-system, BlinkMacSystemFont, sans-serif;
    color: #1a1a1a;
    line-height: 1.6;
    font-size: 13px;
  }
  .name { font-size: 28px; margin: 0 0 6px; font-weight: 700; }
  .contact { color: #555; margin-bottom: 16px; font-size: 12px; }
  .summary { margin: 0 0 10px; }
  .highlights { margin: 0 0 20px; padding-left: 20px; }
  .highlights li { margin-bottom: 6px; }
  .section h2 {
    font-size: 16px;
    margin: 26px 0 12px;
    padding-bottom: 6px;
    border-bottom: 1px solid #333;
  }
  .entry { padding: 14px 0; border-bottom: 1px solid #ddd; }
  .entry:last-child { border-bottom: none; }
  .entry-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    font-weight: 700;
    gap: 12px;
    margin-bottom: 6px;
  }
  .entry-period { font-weight: 400; color: #555; font-size: 12px; white-space: nowrap; }
  .entry-body h3 { font-size: 13px; margin: 10px 0 4px; }
  .entry-body ul { margin: 0 0 8px; padding-left: 20px; }
  .entry-body li { margin-bottom: 3px; }
  .tech-line { font-size: 12px; color: #444; margin-top: 8px; }
  .skill-tags { display: flex; flex-wrap: wrap; gap: 8px; }
  .skill-tag { border: 1px solid #ccc; border-radius: 14px; padding: 4px 12px; font-size: 12px; }
</style>
</head>
<body>
${bodyHtml}
</body>
</html>`);
    printWindow.document.close();
    printWindow.focus();
    setTimeout(() => printWindow.print(), 250);
  }

  function flashButton(btn, text) {
    const original = btn.textContent;
    btn.textContent = text;
    setTimeout(() => (btn.textContent = original), 1200);
  }

  function renderChat() {
    chatMessagesEl.innerHTML = '';
    chatHistory.forEach((msg, index) => renderMessage(msg, index));
    chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;
  }

  function renderMessage(msg, index) {
    const wrapper = document.createElement('div');
    wrapper.className = `chat-message ${msg.role}`;
    wrapper.textContent = msg.content;

    if (msg.role === 'assistant' && msg.updatedResume) {
      const actions = document.createElement('div');
      actions.className = 'resume-diff-actions';

      const previewBtn = document.createElement('button');
      previewBtn.textContent = '미리보기';
      previewBtn.addEventListener('click', () => showPreview(index));
      actions.appendChild(previewBtn);

      if (msg.applied) {
        const label = document.createElement('span');
        label.className = 'applied-label';
        label.textContent = '✓ 이 수정본을 적용했습니다';
        actions.appendChild(label);
      } else {
        const applyBtn = document.createElement('button');
        applyBtn.textContent = '이 수정본 적용';
        applyBtn.addEventListener('click', () => applyUpdatedResume(index));
        actions.appendChild(applyBtn);
      }

      wrapper.appendChild(actions);
    }

    chatMessagesEl.appendChild(wrapper);
  }

  function applyUpdatedResume(index) {
    const msg = chatHistory[index];
    if (!msg || !msg.updatedResume) return;
    resumeInput.value = msg.updatedResume;
    localStorage.setItem(STORAGE_KEYS.resume, msg.updatedResume);
    pushVersion(msg.updatedResume);
    msg.applied = true;
    saveJson(STORAGE_KEYS.chat, chatHistory);
    renderChat();
    if (previewIndex === index) {
      updatePreviewApplyState();
    }
  }

  function showPreview(index) {
    const msg = chatHistory[index];
    if (!msg || !msg.updatedResume) return;
    previewIndex = index;
    previewText.textContent = msg.updatedResume;
    previewPanel.hidden = false;
    appLayout.classList.add('layout--three-col');
    updatePreviewApplyState();
  }

  function closePreview() {
    previewPanel.hidden = true;
    previewIndex = null;
    appLayout.classList.remove('layout--three-col');
  }

  function updatePreviewApplyState() {
    const msg = chatHistory[previewIndex];
    const applied = !!(msg && msg.applied);
    previewApplyBtn.disabled = applied;
    previewApplyBtn.textContent = applied ? '적용됨' : '적용';
  }

  previewCloseBtn.addEventListener('click', closePreview);

  previewApplyBtn.addEventListener('click', () => {
    if (previewIndex !== null) {
      applyUpdatedResume(previewIndex);
    }
  });

  previewCopyBtn.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(previewText.textContent);
      flashButton(previewCopyBtn, '복사됨!');
    } catch (e) {
      flashButton(previewCopyBtn, '복사 실패');
    }
  });

  previewDownloadBtn.addEventListener('click', () => {
    downloadResumeAsPdf(previewText.textContent, previewDownloadBtn);
  });

  function appendMessage(role, content, updatedResume) {
    const msg = { role, content, updatedResume: updatedResume || null, applied: false };
    chatHistory.push(msg);
    saveJson(STORAGE_KEYS.chat, chatHistory);
    renderChat();
  }

  chatForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const text = chatInput.value.trim();
    if (!text) return;

    if (!resumeInput.value.trim()) {
      alert('먼저 이력서를 입력해주세요.');
      return;
    }

    appendMessage('user', text);
    chatInput.value = '';
    chatSendBtn.disabled = true;
    chatInput.disabled = true;

    const thinkingEl = document.createElement('div');
    thinkingEl.className = 'chat-message system';
    thinkingEl.textContent = 'AI가 답변을 작성 중입니다...';
    chatMessagesEl.appendChild(thinkingEl);
    chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;

    try {
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          resume: resumeInput.value,
          jobDescription: jdInput.value,
          messages: chatHistory.map((m) => ({ role: m.role, content: m.content })),
          provider: providerSelect.value
        })
      });

      if (!response.ok) {
        throw new Error(`서버 오류 (${response.status})`);
      }

      const data = await response.json();
      appendMessage('assistant', data.reply, data.updatedResume);
      if (data.updatedResume) {
        showPreview(chatHistory.length - 1);
      }
    } catch (err) {
      appendMessage('system', `오류가 발생했습니다: ${err.message}`);
    } finally {
      thinkingEl.remove();
      chatSendBtn.disabled = false;
      chatInput.disabled = false;
      chatInput.focus();
    }
  });

  chatInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      chatForm.requestSubmit();
    }
  });

  initFields();
  renderVersions();
  renderChat();
})();
