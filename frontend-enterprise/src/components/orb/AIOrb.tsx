import { useRef, useMemo, useState, useEffect, Suspense } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { motion } from 'framer-motion';

function detectWebGL(): boolean {
  try {
    const canvas = document.createElement('canvas');
    return !!(
      window.WebGLRenderingContext &&
      (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
    );
  } catch {
    return false;
  }
}

const PARTICLE_COUNT = 80;

function OrbMesh({ streaming }: { streaming: boolean }) {
  const meshRef = useRef<THREE.Mesh>(null);
  const particlesRef = useRef<THREE.Points>(null);
  const timeRef = useRef(0);

  const particles = useMemo(() => {
    const positions = new Float32Array(PARTICLE_COUNT * 3);
    for (let i = 0; i < PARTICLE_COUNT; i++) {
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(2 * Math.random() - 1);
      const r = 0.6 + Math.random() * 0.4;
      positions[i * 3]     = r * Math.sin(phi) * Math.cos(theta);
      positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      positions[i * 3 + 2] = r * Math.cos(phi);
    }
    return positions;
  }, []);

  useFrame((_, delta) => {
    timeRef.current += delta;
    if (meshRef.current) {
      meshRef.current.rotation.y += streaming ? delta * 2.0 : delta * 0.3;
      meshRef.current.rotation.x = Math.sin(timeRef.current * 0.5) * 0.1;
      meshRef.current.scale.setScalar(
        streaming ? 1 + Math.sin(timeRef.current * 4) * 0.08 : 1
      );
      const mat = meshRef.current.material as THREE.MeshStandardMaterial;
      mat.emissiveIntensity = streaming ? 0.6 : 0.25;
    }
    if (particlesRef.current) {
      particlesRef.current.rotation.y -= delta * 0.5;
      particlesRef.current.visible = streaming;
    }
  });

  return (
    <>
      <pointLight position={[0, 2, 2]} intensity={2} color="#19D3FF" />
      <pointLight position={[0, -2, -1]} intensity={0.5} color="#6AE3FF" />
      <ambientLight intensity={0.3} />

      <mesh ref={meshRef}>
        <sphereGeometry args={[0.38, 32, 32]} />
        <meshStandardMaterial
          color="#050505"
          emissive="#19D3FF"
          emissiveIntensity={0.25}
          roughness={0.1}
          metalness={0.8}
        />
      </mesh>

      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[0.5, 0.012, 8, 64]} />
        <meshStandardMaterial
          color="#19D3FF"
          emissive="#19D3FF"
          emissiveIntensity={streaming ? 1.5 : 0.5}
          transparent
          opacity={streaming ? 0.8 : 0.4}
        />
      </mesh>

      {/* Particles — only visible during streaming */}
      <points ref={particlesRef} visible={false}>
        <bufferGeometry>
          {/* R3F v8: use count/array/itemSize props, not args */}
          <bufferAttribute
            attach="attributes-position"
            count={PARTICLE_COUNT}
            array={particles}
            itemSize={3}
          />
        </bufferGeometry>
        <pointsMaterial
          size={0.015}
          color="#19D3FF"
          transparent
          opacity={0.7}
          sizeAttenuation
        />
      </points>
    </>
  );
}

function CSSOrb({ streaming, size }: { streaming: boolean; size: number }) {
  const inner = size * 0.72;
  return (
    <div
      style={{ width: size, height: size }}
      className="flex items-center justify-center"
    >
      <motion.div
        animate={
          streaming
            ? { scale: [1, 1.14, 1], opacity: [0.75, 1, 0.75] }
            : { scale: [1, 1.05, 1], opacity: [0.55, 0.85, 0.55] }
        }
        transition={{
          duration: streaming ? 0.9 : 3,
          repeat: Infinity,
          ease: 'easeInOut',
        }}
        style={{
          width: inner,
          height: inner,
          borderRadius: '50%',
          background:
            'radial-gradient(circle at 33% 33%, #6AE3FF 0%, #19D3FF 45%, #083040 100%)',
          boxShadow: streaming
            ? `0 0 ${size * 0.22}px #19D3FF, 0 0 ${size * 0.45}px rgba(25,211,255,0.35)`
            : `0 0 ${size * 0.1}px #19D3FF, 0 0 ${size * 0.22}px rgba(25,211,255,0.18)`,
        }}
      />
    </div>
  );
}

export interface AIOrbProps {
  streaming?: boolean;
  /** Pixel size of the square container. Default 120. */
  size?: number;
}

export function AIOrb({ streaming = false, size = 120 }: AIOrbProps) {
  const [webGL, setWebGL] = useState<boolean | null>(null);

  useEffect(() => {
    setWebGL(detectWebGL());
  }, []);

  // Hydration placeholder
  if (webGL === null) return <div style={{ width: size, height: size }} />;

  // CSS fallback
  if (!webGL) return <CSSOrb streaming={streaming} size={size} />;

  return (
    <div
      style={{
        width: size,
        height: size,
        position: 'relative',
        flexShrink: 0,
      }}
    >
      {/* Radial glow behind canvas */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          borderRadius: '50%',
          background: streaming
            ? 'radial-gradient(circle, rgba(25,211,255,0.22) 0%, transparent 70%)'
            : 'radial-gradient(circle, rgba(25,211,255,0.09) 0%, transparent 70%)',
          transition: 'background 0.5s ease',
          pointerEvents: 'none',
        }}
      />

      <Suspense fallback={<CSSOrb streaming={streaming} size={size} />}>
        <Canvas
          camera={{ position: [0, 0, 2], fov: 50 }}
          gl={{ antialias: true, alpha: true }}
          dpr={[1, Math.min(window.devicePixelRatio, 2)]}
          style={{
            width: size,
            height: size,
            display: 'block',
            background: 'transparent',
          }}
        >
          <OrbMesh streaming={streaming} />
        </Canvas>
      </Suspense>
    </div>
  );
}
