import spaceport.launchpad.element.CSS
import spaceport.launchpad.element.Element
import spaceport.launchpad.element.Javascript

class CrtKnob implements Element {

    String prerender(String body, Map attributes) {
        return "<div class='knob-wrap crt-knob-wrap'>" +
                   "<div class='knob' data-value='50'><div class='knob-indicator'></div></div>" +
                   "<div class='knob-label'>CRT</div>" +
               "</div>"
    }

    @Javascript
    def constructed = /* language=javascript */ """
        function(element) {
            const knob = element.querySelector('.knob');
            const STORAGE_KEY = 'zzgod-crt-intensity';
            let value = parseInt(localStorage.getItem(STORAGE_KEY) ?? '50', 10);
            let dragging = false;
            let startX = 0;
            let startValue = 0;

            function update() {
                const angle = -135 + (value / 100) * 270;
                knob.style.transform = 'rotate(' + angle + 'deg)';
                const viewport = document.getElementById('game-viewport');
                if (viewport) {
                    viewport.style.setProperty('--crt-intensity', (value / 100).toString());
                }
                knob.dataset.value = value;
                localStorage.setItem(STORAGE_KEY, value);
            }

            update();

            knob.addEventListener('mousedown', (e) => {
                e.preventDefault();
                dragging = true;
                startX = e.clientX;
                startValue = value;
            });

            window.addEventListener('mousemove', (e) => {
                if (!dragging) return;
                const delta = e.clientX - startX;
                value = Math.max(0, Math.min(100, startValue + Math.round(delta / 2)));
                update();
            });

            window.addEventListener('mouseup', () => {
                dragging = false;
            });
        }
    """

    @CSS
    def style = /* language=css */ """
        .crt-knob-wrap .knob-indicator {
            background: var(--neon-purple, #bf00ff);
            box-shadow: 0 0 6px var(--neon-purple, #bf00ff);
        }
        .crt-knob-wrap .knob:hover {
            border-color: var(--neon-purple, #bf00ff);
            box-shadow: 0 0 10px rgba(191, 0, 255, 0.15), inset 0 0 8px rgba(0, 0, 0, 0.4);
        }
    """

}
