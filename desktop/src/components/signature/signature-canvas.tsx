import { useRef, useEffect } from "react";
import { useSignaturePadStore, type Stroke } from "@/stores/signature-pad-store";

interface SignatureCanvasProps {
  width?: number;
  height?: number;
  className?: string;
}

/**
 * Renders signature strokes live as they arrive from the Android device.
 * Uses an SVG element for crisp vector rendering at any scale.
 */
export function SignatureCanvas({
  width = 400,
  height = 200,
  className,
}: SignatureCanvasProps) {
  const { strokes, currentStroke } = useSignaturePadStore();
  const svgRef = useRef<SVGSVGElement>(null);

  // Auto-scroll/zoom not needed — we render at the canvas coordinate space
  useEffect(() => {
    // No-op: the SVG re-renders reactively when strokes change
  }, [strokes, currentStroke]);

  const allStrokes: Stroke[] = [...strokes];
  if (currentStroke.length > 0) {
    allStrokes.push({ points: currentStroke });
  }

  return (
    <div
      className={`relative rounded-lg border-2 border-dashed border-border bg-white ${className ?? ""}`}
      style={{ width: "100%", aspectRatio: `${width}/${height}` }}
    >
      <svg
        ref={svgRef}
        width="100%"
        height="100%"
        viewBox={`0 0 ${width} ${height}`}
        preserveAspectRatio="xMidYMid meet"
        className="absolute inset-0"
      >
        <rect width={width} height={height} fill="white" />
        {allStrokes.map((stroke, i) => {
          if (stroke.points.length === 0) return null;
          const d = stroke.points
            .map((p, j) => {
              if (j === 0) return `M ${p.x.toFixed(2)} ${p.y.toFixed(2)}`;
              return `L ${p.x.toFixed(2)} ${p.y.toFixed(2)}`;
            })
            .join(" ");
          return (
            <path
              key={i}
              d={d}
              stroke="#1a1a2e"
              strokeWidth={2}
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          );
        })}
      </svg>
      {allStrokes.length === 0 && (
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <span className="text-sm text-muted-foreground">
            En attente de signature...
          </span>
        </div>
      )}
    </div>
  );
}
