import { useRef, useMemo, Suspense } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';

const PARTICLE_COUNT = 80;

function OrbScene({ streaming }: { streaming: boolean }) {
  const coreRef  = useRef<THREE.Mesh>(null);
  const wireRef  = useRef<THREE.Mesh>(null);
  const glowRef  = useRef<THREE.Mesh>(null);
  const ptRef    = useRef<THREE.Points>(null);
  const t        = useRef(0);

  const particles = useMemo(() => {
    const pos = new Float32Array(PARTICLE_COUNT * 3);
    for (let i = 0; i < PARTICLE_COUNT; i++) {
      const theta = Math.random() * Math.PI * 2;
      const phi   = Math.acos(2 * Math.random() - 1);
      const r     = 0.72 + Math.random() * 0.18;
      pos[i * 3]     = r * Math.sin(phi) * Math.cos(theta);
      pos[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      pos[i * 3 + 2] = r * Math.cos(phi);
    }
    return pos;
  }, []);

  useFrame((_, delta) => {
    t.current += delta;
    const speed = streaming ? 1.4 : 0.22;

    if (coreRef.current) {
      coreRef.current.rotation.y += delta * speed;
      coreRef.current.rotation.x  = Math.sin(t.current * 0.4) * 0.1;
      if (streaming) {
        coreRef.current.scale.setScalar(1 + Math.sin(t.current * 4) * 0.07);
      } else {
        coreRef.current.scale.setScalar(1);
      }
      const mat = coreRef.current.material as THREE.MeshStandardMaterial;
      mat.emissiveIntensity = streaming
        ? 0.55 + Math.sin(t.current * 3) * 0.15
        : 0.28;
    }

    if (wireRef.current) {
      wireRef.current.rotation.y += delta * speed * 0.7;
      wireRef.current.rotation.x  = Math.cos(t.current * 0.3) * 0.08;
      const wmat = wireRef.current.material as THREE.MeshBasicMaterial;
      wmat.opacity = streaming ? 0.45 + Math.sin(t.current * 2) * 0.1 : 0.22;
    }

    if (glowRef.current) {
      glowRef.current.rotation.y -= delta * 0.1;
      const gmat = glowRef.current.material as THREE.MeshBasicMaterial;
      gmat.opacity = streaming ? 0.07 + Math.sin(t.current * 2) * 0.02 : 0.04;
    }

    if (ptRef.current) {
      ptRef.current.visible          = streaming;
      ptRef.current.rotation.y      -= delta * 0.6;
    }
  });

  return (
    <>
      {/* Lights */}
      <ambientLight intensity={0.25} />
      <pointLight position={[1.5,  2.5,  2]}  intensity={3.5} color="#19D3FF" />
      <pointLight position={[-2, -1.5, -1]}   intensity={1.2} color="#6AE3FF" />
      <pointLight position={[0,   0,    2.5]} intensity={0.6} color="#ffffff" />

      {/* Core sphere */}
      <mesh ref={coreRef}>
        <sphereGeometry args={[0.42, 64, 64]} />
        <meshStandardMaterial
          color="#020d15"
          emissive="#19D3FF"
          emissiveIntensity={0.28}
          roughness={0.05}
          metalness={0.92}
        />
      </mesh>

      {/* Icosahedron network wireframe — gives the AI-sphere lattice look */}
      <mesh ref={wireRef}>
        <icosahedronGeometry args={[0.56, 2]} />
        <meshBasicMaterial
          color="#19D3FF"
          wireframe
          transparent
          opacity={0.22}
        />
      </mesh>

      {/* Outer atmospheric glow sphere */}
      <mesh ref={glowRef}>
        <sphereGeometry args={[0.72, 32, 32]} />
        <meshBasicMaterial
          color="#19D3FF"
          transparent
          opacity={0.04}
          side={THREE.BackSide}
        />
      </mesh>

      {/* Equatorial ring */}
      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[0.56, 0.008, 8, 80]} />
        <meshStandardMaterial
          color="#19D3FF"
          emissive="#19D3FF"
          emissiveIntensity={streaming ? 2.2 : 0.7}
          transparent
          opacity={streaming ? 0.85 : 0.45}
        />
      </mesh>

      {/* Streaming particles */}
      <points ref={ptRef} visible={false}>
        <bufferGeometry>
          <bufferAttribute
            attach="attributes-position"
            args={[particles, 3]}
          />
        </bufferGeometry>
        <pointsMaterial
          size={0.016}
          color="#6AE3FF"
          transparent
          opacity={0.75}
          sizeAttenuation
        />
      </points>
    </>
  );
}

export interface AIOrbProps {
  streaming?: boolean;
  size?: number;
}

export function AIOrb({ streaming = false, size = 300 }: AIOrbProps) {
  return (
    <div style={{ width: size, height: size, position: 'relative', flexShrink: 0 }}>
      {/* Radial glow layer behind the canvas */}
      <div
        style={{
          position: 'absolute',
          inset: -size * 0.15,
          borderRadius: '50%',
          background: streaming
            ? 'radial-gradient(circle, rgba(25,211,255,0.18) 0%, transparent 65%)'
            : 'radial-gradient(circle, rgba(25,211,255,0.08) 0%, transparent 65%)',
          transition: 'background 0.6s ease',
          pointerEvents: 'none',
        }}
      />

      <Suspense fallback={null}>
        <Canvas
          camera={{ position: [0, 0, 1.9], fov: 52 }}
          gl={{ antialias: true, alpha: true }}
          dpr={[1, Math.min(window.devicePixelRatio, 2)]}
          style={{
            width:      size,
            height:     size,
            display:    'block',
            background: 'transparent',
          }}
        >
          <OrbScene streaming={streaming} />
        </Canvas>
      </Suspense>
    </div>
  );
}
